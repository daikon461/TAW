package dev.dualpower;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.SkillData;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Creates a fresh second-awakening candidate in memory only. No player data writes. */
public final class DetachedSecondCandidate {
    private DetachedSecondCandidate() {}

    /** Reject stale slot1 snapshots before committing a second ability. */
    private static boolean primaryMatchesLive(ServerPlayer player) {
        CompoundTag saved = player.getPersistentData()
                .getCompound("awakened_dual_power_codec_slots_v1").getCompound("slot1");
        if (!saved.contains("power") || !saved.contains("skill")) return false;
        Tag livePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                AwakenedAttachments.get(player)).result().orElseThrow();
        Tag liveSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                AwakenedAttachments.skill(player)).result().orElseThrow();
        return livePower.equals(saved.get("power")) && liveSkill.equals(saved.get("skill"));
    }

    public static String preview(ServerPlayer player, String requestedId) {
        try {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot"))
                return "第2能力候補の生成中止: 未完了の復旧記録があります";
            String first = data.getString("awakened_dual_power_primary_id");
            String live = AwakenedAttachments.get(player).powerId();
            if (first.isBlank() || !first.equals(live))
                return "生成中止: 第1能力の登録と現在の能力が一致しません";
            if (!FullAwakenedSnapshot.verifySlot(player, 1).contains("検証OK"))
                return "生成中止: 第1能力のCodec保存を検証できません";
            if (!primaryMatchesLive(player))
                return "生成中止: 第1スロットが現在の能力・スキルと不一致です。/dualpower codecslots save1 で更新してください";
            if (!data.getString("awakened_dual_power_second").isBlank()
                    || data.getCompound("awakened_dual_power_codec_slots_v1").contains("slot2", Tag.TAG_COMPOUND))
                return "生成中止: 第2能力の登録または保存データが既にあります。上書きしません";
            String id = PowerIdValidator.requireValid(requestedId);
            if (PowerRegistry.get(id) == null)
                return "生成中止: Awakenedに存在しない能力IDです: " + id;
            if (id.equals(first) || id.equals(PowerData.NONE))
                return "生成中止: 第1能力と同一、または空能力です";
            // All mutation below is on NEW, detached objects, never on Awakened attachments.
            PowerData candidatePower = new PowerData();
            candidatePower.assignPower(id, true);
            SkillData candidateSkill = new SkillData();
            Tag powerTag = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, candidatePower)
                    .result().orElseThrow(() -> new IllegalStateException("candidate power encode"));
            Tag skillTag = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, candidateSkill)
                    .result().orElseThrow(() -> new IllegalStateException("candidate skill encode"));
            PowerData decodedPower = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, powerTag)
                    .result().orElseThrow(() -> new IllegalStateException("candidate power decode"));
            SkillData decodedSkill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, skillTag)
                    .result().orElseThrow(() -> new IllegalStateException("candidate skill decode"));
            if (!id.equals(decodedPower.powerId()) || decodedSkill.level() != candidateSkill.level())
                return "生成中止: 第2能力候補の往復検証に失敗";
            if (!powerTag.equals(PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, decodedPower)
                    .result().orElseThrow()) || !skillTag.equals(SkillData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, decodedSkill).result().orElseThrow()))
                return "生成中止: 第2能力候補の再エンコード不一致";
            return "第2能力候補のメモリ内生成OK: " + id + " / 初期レベル=" + decodedSkill.level()
                    + "。能力IDはAwakenedの登録情報で検証済み。保存・覚醒・結晶消費・能力変更は行っていません";
        } catch (Exception ex) {
            return "第2能力候補の生成失敗: " + ex.getClass().getSimpleName() + "（変更なし）";
        }
    }
    /** Experimental detached slot creation. Does not touch live Awakened attachments. */
    public static String saveCandidate(ServerPlayer player, String requestedId) {
        CompoundTag data = player.getPersistentData();
        final String id;
        try { id = PowerIdValidator.requireValid(requestedId); }
        catch (Exception ex) { return "保存中止: 能力IDの形式が不正です"; }
        if (data.getBoolean("awakened_dual_power_transaction_pending")
                || data.contains("awakened_dual_power_recovery_snapshot"))
            return "保存中止: 未完了の復旧記録があります";
        String first = data.getString("awakened_dual_power_primary_id");
        if (PowerRegistry.get(id) == null)
            return "保存中止: Awakenedに存在しない能力IDです: " + id;
        if (first.isBlank() || !first.equals(AwakenedAttachments.get(player).powerId())
                || first.equals(id) || PowerData.NONE.equals(id))
            return "保存中止: 第1能力の登録状態または候補IDが不正です";
        if (!FullAwakenedSnapshot.verifySlot(player, 1).contains("検証OK"))
            return "保存中止: 第1能力の保存データが検証できません";
        try {
            if (!primaryMatchesLive(player))
                return "保存中止: 第1スロットが現在の能力・スキルと不一致です。/dualpower codecslots save1 で更新してください";
        } catch (Exception ex) {
            return "保存中止: 第1能力の現行データ照合に失敗（変更なし）";
        }
        String secondKey = "awakened_dual_power_second";
        String slotsKey = "awakened_dual_power_codec_slots_v1";
        if (!data.getString(secondKey).isBlank()
                || data.getCompound(slotsKey).contains("slot2", Tag.TAG_COMPOUND))
            return "保存中止: 第2能力は既に登録済みです（上書き禁止）";
        // Snapshot current live data before any persistent-data changes.
        try {
            Tag livePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag liveSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            PowerData candidate = new PowerData();
            candidate.assignPower(id, true);
            SkillData candidateSkill = new SkillData();
            Tag power = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, candidate)
                    .result().orElseThrow();
            Tag skill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, candidateSkill)
                    .result().orElseThrow();
            PowerData restoredPower = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, power)
                    .result().orElseThrow();
            SkillData restoredSkill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, skill)
                    .result().orElseThrow();
            if (!id.equals(restoredPower.powerId())
                    || !power.equals(PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, restoredPower).result().orElseThrow())
                    || !skill.equals(SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, restoredSkill).result().orElseThrow()))
                return "保存中止: 候補データのCodec往復検証に失敗";
            CompoundTag saved = new CompoundTag();
            saved.putInt("schema", 1);
            saved.putString("power_id", id);
            saved.put("power", power.copy());
            saved.put("skill", skill.copy());
            CompoundTag newSlots = data.getCompound(slotsKey).copy();
            newSlots.put("slot2", saved);
            // Commit both keys; revert only keys touched here on any verification failure.
            boolean hadSecond = data.contains(secondKey);
            Tag oldSecond = hadSecond ? data.get(secondKey).copy() : null;
            boolean hadSlots = data.contains(slotsKey);
            Tag oldSlots = hadSlots ? data.get(slotsKey).copy() : null;
            try {
                data.put(slotsKey, newSlots);
                data.putString(secondKey, id);
                if (!FullAwakenedSnapshot.verifySlot(player, 2).contains("検証OK"))
                    throw new IllegalStateException("slot2 verification failed");
                Tag afterPower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                        AwakenedAttachments.get(player)).result().orElseThrow();
                Tag afterSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                        AwakenedAttachments.skill(player)).result().orElseThrow();
                if (!livePower.equals(afterPower) || !liveSkill.equals(afterSkill))
                    throw new IllegalStateException("live data changed");
            } catch (Exception failure) {
                if (hadSecond) data.put(secondKey, oldSecond); else data.remove(secondKey);
                if (hadSlots) data.put(slotsKey, oldSlots); else data.remove(slotsKey);
                return "第2スロット保存失敗: 登録情報をロールバックしました（" + failure.getClass().getSimpleName() + "）";
            }
            return "第2能力候補をスロット2へ保存しました: " + id
                    + "。能力IDはAwakenedの登録情報で検証済み。実際の覚醒・能力切り替え・結晶消費は無効です";
        } catch (Exception ex) {
            return "第2スロット保存中止: " + ex.getClass().getSimpleName() + "（変更なし）";
        }
    }

}
