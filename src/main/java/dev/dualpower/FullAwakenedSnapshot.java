package dev.dualpower;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.SkillData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Read-only serialization audit. Never mutates an Awakened attachment. */
public final class FullAwakenedSnapshot {
    private FullAwakenedSnapshot() {}
    public static final String KEY = "awakened_dual_power_full_snapshot_audit_v1";

    /** Validates a saved snapshot without changing either Awakened attachment. */
    public static String verify(ServerPlayer player) {
        try {
            CompoundTag data = player.getPersistentData();
            if (!data.contains(KEY, Tag.TAG_COMPOUND)) return "保存済み全データがありません。Alt+Vで安全スナップショットを作成してください";
            CompoundTag saved = data.getCompound(KEY);
            if (saved.getInt("schema") != 1 || !saved.contains("power") || !saved.contains("skill"))
                return "保存形式が不正です。能力変更は実行しません";
            PowerData oldPower = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved.get("power"))
                .result().orElseThrow(() -> new IllegalStateException("Saved power decode failed"));
            SkillData oldSkill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved.get("skill"))
                .result().orElseThrow(() -> new IllegalStateException("Saved skill decode failed"));
            if (!saved.getString("power_id").equals(oldPower.powerId()))
                return "保存済み能力IDが一致しません。能力変更は実行しません";
            Tag currentPower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, AwakenedAttachments.get(player))
                .result().orElseThrow(() -> new IllegalStateException("Current power encode failed"));
            Tag currentSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, AwakenedAttachments.skill(player))
                .result().orElseThrow(() -> new IllegalStateException("Current skill encode failed"));
            boolean equal = saved.get("power").equals(currentPower) && saved.get("skill").equals(currentSkill);
            return equal ? "保存時と現在のPowerData・SkillDataは一致しています（読み取り専用）"
                : "保存時から能力またはスキルが変化しています（通常の育成でも変化します）。復元・切り替えは実行しません";
        } catch (Exception ex) {
            return "保存済み全データの検証失敗: " + ex.getClass().getSimpleName() + "（能力変更なし）";
        }
    }

    public static String capture(ServerPlayer player) {
        try {
            PowerData power = AwakenedAttachments.get(player);
            SkillData skill = AwakenedAttachments.skill(player);
            Tag powerTag = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, power)
                .result().orElseThrow(() -> new IllegalStateException("PowerData encode failed"));
            Tag skillTag = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, skill)
                .result().orElseThrow(() -> new IllegalStateException("SkillData encode failed"));
            // A round-trip decode catches a surprising class of corrupt snapshots.
            PowerData decodedPower = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, powerTag)
                .result().orElseThrow(() -> new IllegalStateException("PowerData decode failed"));
            SkillData decodedSkill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, skillTag)
                .result().orElseThrow(() -> new IllegalStateException("SkillData decode failed"));
            if (!power.powerId().equals(decodedPower.powerId())
                    || skill.level() != decodedSkill.level()
                    || Double.compare(skill.xp(), decodedSkill.xp()) != 0) {
                return "シリアライズ往復検証に失敗。記録は変更していません";
            }
            CompoundTag snapshot = new CompoundTag();
            snapshot.putInt("schema", 1);
            snapshot.putString("power_id", power.powerId());
            snapshot.put("power", powerTag.copy());
            snapshot.put("skill", skillTag.copy());
            // Store only the currently equipped power. This is a forensic backup,
            // not a switchable slot; it must never be applied automatically.
            player.getPersistentData().put(KEY, snapshot);
            return "PowerDataとSkillDataの全Codecデータを検証・保存しました（切り替えは無効）";
        } catch (Exception ex) {
            return "全データ保存失敗: " + ex.getClass().getSimpleName() + "（能力変更なし）";
        }
    }
    /** Save a complete Codec snapshot to the slot matching the CURRENT power ID only. */
    public static String captureSlot(ServerPlayer player, int slot) {
        if (slot != 1 && slot != 2) return "不正なスロット番号";
        String idKey = slot == 1 ? "awakened_dual_power_primary_id" : "awakened_dual_power_second";
        String expected = player.getPersistentData().getString(idKey);
        String current = AwakenedAttachments.get(player).powerId();
        if (expected.isBlank() || !expected.equals(current))
            return "保存中止: スロット" + slot + "の登録能力と現在の能力が一致しません";
        String result = capture(player);
        if (!result.startsWith("PowerDataとSkillData")) return result;
        CompoundTag data = player.getPersistentData();
        CompoundTag full = data.getCompound(KEY).copy();
        if (!expected.equals(full.getString("power_id"))) return "保存中止: 能力IDの再確認に失敗";
        CompoundTag slots = data.getCompound("awakened_dual_power_codec_slots_v1").copy();
        String name = "slot" + slot;
        if (slots.contains(name, Tag.TAG_COMPOUND)) {
            CompoundTag backups = data.getCompound("awakened_dual_power_codec_slots_backup_v1").copy();
            backups.put(name, slots.getCompound(name).copy());
            data.put("awakened_dual_power_codec_slots_backup_v1", backups);
        }
        slots.put(name, full);
        data.put("awakened_dual_power_codec_slots_v1", slots);
        return "スロット" + slot + "のCodecデータを保存しました（読み取り専用・能力変更なし）";
    }

    /** Decode and validate a slot independently; never applies its contents. */
    public static String verifySlot(ServerPlayer player, int slot) {
        if (slot != 1 && slot != 2) return "不正なスロット番号";
        try {
            CompoundTag data = player.getPersistentData();
            String expected = data.getString(slot == 1 ? "awakened_dual_power_primary_id" : "awakened_dual_power_second");
            CompoundTag slots = data.getCompound("awakened_dual_power_codec_slots_v1");
            String name = "slot" + slot;
            if (!slots.contains(name, Tag.TAG_COMPOUND)) return "スロット" + slot + "にCodec保存データがありません";
            CompoundTag saved = slots.getCompound(name);
            if (saved.getInt("schema") != 1 || expected.isBlank() || !expected.equals(saved.getString("power_id")))
                return "スロット" + slot + "の形式または登録能力IDが不一致";
            PowerData power = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved.get("power"))
                .result().orElseThrow(() -> new IllegalStateException("Power decode failed"));
            SkillData skill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved.get("skill"))
                .result().orElseThrow(() -> new IllegalStateException("Skill decode failed"));
            if (!expected.equals(power.powerId())) return "スロット" + slot + "の内部能力IDが不一致";
            Tag powerRoundtrip = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, power)
                .result().orElseThrow(() -> new IllegalStateException("Power re-encode failed"));
            Tag skillRoundtrip = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, skill)
                .result().orElseThrow(() -> new IllegalStateException("Skill re-encode failed"));
            if (!saved.get("power").equals(powerRoundtrip) || !saved.get("skill").equals(skillRoundtrip))
                return "スロット" + slot + "のCodec再エンコードが不一致（切り替え禁止）";
            return "スロット" + slot + "の保存データ検証OK（レベル" + skill.level() + "、能力変更なし）";
        } catch (Exception ex) {
            return "スロット" + slot + "の検証失敗: " + ex.getClass().getSimpleName() + "（能力変更なし）";
        }
    }

}
