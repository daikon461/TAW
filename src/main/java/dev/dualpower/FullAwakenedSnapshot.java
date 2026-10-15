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

    /** Checks the previous slot generation without touching the live Awakened attachment. */
    public static String verifyBackup(ServerPlayer player, int slot) {
        if (slot != 1 && slot != 2) return "不正なスロット番号";
        try {
            CompoundTag data = player.getPersistentData();
            CompoundTag backups = data.getCompound("awakened_dual_power_codec_slots_backup_v1");
            String name = "slot" + slot;
            if (!backups.contains(name, Tag.TAG_COMPOUND)) return "スロット" + slot + "の前世代バックアップはありません";
            CompoundTag saved = backups.getCompound(name);
            String expected = data.getString(slot == 1 ? "awakened_dual_power_primary_id" : "awakened_dual_power_second");
            if (expected.isBlank() || saved.getInt("schema") != 1 || !expected.equals(saved.getString("power_id")))
                return "前世代バックアップの登録能力IDまたは形式が不一致";
            PowerData power = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved.get("power"))
                .result().orElseThrow(() -> new IllegalStateException("backup power decode"));
            SkillData skill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved.get("skill"))
                .result().orElseThrow(() -> new IllegalStateException("backup skill decode"));
            if (!expected.equals(power.powerId())) return "前世代バックアップの内部能力IDが不一致";
            Tag p = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, power)
                .result().orElseThrow(() -> new IllegalStateException("backup power encode"));
            Tag k = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, skill)
                .result().orElseThrow(() -> new IllegalStateException("backup skill encode"));
            if (!p.equals(saved.get("power")) || !k.equals(saved.get("skill")))
                return "前世代バックアップの往復検証に失敗";
            return "スロット" + slot + "の前世代バックアップ検証OK（レベル" + skill.level() + "、書き換えなし）";
        } catch (Exception ex) {
            return "前世代バックアップ検証失敗: " + ex.getClass().getSimpleName() + "（書き換えなし）";
        }
    }

    /** Non-mutating restore rehearsal: decode and re-encode both slots and current attachments. */
    public static String rehearseRestore(ServerPlayer player) {
        try {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot"))
                return "復元予行演習を停止: 未完了の復旧記録があります";
            String currentId = AwakenedAttachments.get(player).powerId();
            if (currentId.isBlank()) return "復元予行演習を停止: 現在の能力IDが空です";
            Tag livePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, AwakenedAttachments.get(player))
                .result().orElseThrow(() -> new IllegalStateException("live power encode"));
            Tag liveSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, AwakenedAttachments.skill(player))
                .result().orElseThrow(() -> new IllegalStateException("live skill encode"));
            PowerData decodedLive = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, livePower)
                .result().orElseThrow(() -> new IllegalStateException("live power decode"));
            SkillData decodedSkill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, liveSkill)
                .result().orElseThrow(() -> new IllegalStateException("live skill decode"));
            if (!currentId.equals(decodedLive.powerId())
                    || decodedSkill.level() != AwakenedAttachments.skill(player).level())
                return "復元予行演習を停止: 現在のデータが往復検証に失敗";
            String a = verifySlot(player, 1);
            String b = verifySlot(player, 2);
            if (!a.contains("検証OK") || !b.contains("検証OK"))
                return "復元予行演習: 未準備。第1=" + a + " / 第2=" + b;
            String id1 = data.getString("awakened_dual_power_primary_id");
            String id2 = data.getString("awakened_dual_power_second");
            if (id1.isBlank() || id2.isBlank() || id1.equals(id2))
                return "復元予行演習を停止: 2つの能力IDが未設定または重複";
            return "復元予行演習OK: 2スロットと現在データのCodec検証成功。ただし実際の復元・切り替えの安全性は未確認（変更なし）";
        } catch (Exception ex) {
            return "復元予行演習失敗: " + ex.getClass().getSimpleName() + "（能力変更なし）";
        }
    }

    /** Decode a stored slot into detached objects and compare its canonical Codec data.
     * This does NOT attach decoded objects to the player or mutate live data. */
    public static String stageRestore(ServerPlayer player, int slot) {
        if (slot != 1 && slot != 2) return "不正なスロット番号";
        try {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot"))
                return "復元ステージング中止: 未完了の復旧記録があります";
            String verified = verifySlot(player, slot);
            if (!verified.contains("検証OK")) return "復元ステージング中止: " + verified;
            CompoundTag saved = data.getCompound("awakened_dual_power_codec_slots_v1")
                    .getCompound("slot" + slot);
            // Freshly decoded objects are detached; no calls to assignPower or attachment setters.
            PowerData stagedPower = PowerData.MAP_CODEC.codec()
                    .parse(NbtOps.INSTANCE, saved.get("power")).result()
                    .orElseThrow(() -> new IllegalStateException("staged power decode"));
            SkillData stagedSkill = SkillData.MAP_CODEC.codec()
                    .parse(NbtOps.INSTANCE, saved.get("skill")).result()
                    .orElseThrow(() -> new IllegalStateException("staged skill decode"));
            Tag stagedPowerTag = PowerData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, stagedPower).result()
                    .orElseThrow(() -> new IllegalStateException("staged power encode"));
            Tag stagedSkillTag = SkillData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, stagedSkill).result()
                    .orElseThrow(() -> new IllegalStateException("staged skill encode"));
            if (!stagedPowerTag.equals(saved.get("power")) || !stagedSkillTag.equals(saved.get("skill")))
                return "復元ステージング失敗: 保存内容と再エンコード結果が不一致";
            String liveId = AwakenedAttachments.get(player).powerId();
            return "スロット" + slot + "の復元ステージングOK: 能力=" + stagedPower.powerId()
                    + " / レベル=" + stagedSkill.level()
                    + " / 現在の能力=" + liveId
                    + "（切り離したオブジェクトのみ生成・プレイヤーデータ変更なし）";
        } catch (Exception ex) {
            return "復元ステージング失敗: " + ex.getClass().getSimpleName() + "（変更なし）";
        }
    }


    /** Read-only audit: capture live Codec state before and after staging both slots. */
    public static String auditLiveIntegrity(ServerPlayer player) {
        try {
            CompoundTag persistent = player.getPersistentData();
            // Compare the whole persistent data too: a read-only audit must not change bookkeeping.
            CompoundTag persistentBefore = persistent.copy();
            Tag powerBefore = PowerData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, AwakenedAttachments.get(player)).result()
                    .orElseThrow(() -> new IllegalStateException("live power before encode"));
            Tag skillBefore = SkillData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, AwakenedAttachments.skill(player)).result()
                    .orElseThrow(() -> new IllegalStateException("live skill before encode"));
            String slot1 = stageRestore(player, 1);
            String slot2 = stageRestore(player, 2);
            Tag powerAfter = PowerData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, AwakenedAttachments.get(player)).result()
                    .orElseThrow(() -> new IllegalStateException("live power after encode"));
            Tag skillAfter = SkillData.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, AwakenedAttachments.skill(player)).result()
                    .orElseThrow(() -> new IllegalStateException("live skill after encode"));
            boolean unchanged = powerBefore.equals(powerAfter) && skillBefore.equals(skillAfter)
                    && persistentBefore.equals(persistent);
            if (!unchanged) return "整合性監査NG: 検査前後のデータが不一致。実際の切り替えは禁止。";
            boolean firstReady = slot1.contains("復元ステージングOK");
            boolean secondReady = slot2.contains("復元ステージングOK");
            return "整合性監査: 現在の能力・スキル・保存領域は検査前後で一致。第1="
                    + (firstReady ? "準備OK" : "未準備(" + slot1 + ")") + " / 第2="
                    + (secondReady ? "準備OK" : "未準備(" + slot2 + ")")
                    + "。実際の復元安全性は未検証（能力変更なし）";
        } catch (Exception ex) {
            return "整合性監査失敗: " + ex.getClass().getSimpleName() + "（切り替え禁止）";
        }
    }

    /** Simulates both switch directions on detached Codec objects only. No attachment setter is called. */
    public static String simulateDetachedSwitch(ServerPlayer player) {
        try {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot"))
                return "模擬切替中止: 未完了の復旧記録があります";
            CompoundTag beforeData = data.copy();
            Tag beforePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag beforeSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            String first = verifySlot(player, 1);
            String second = verifySlot(player, 2);
            if (!first.contains("検証OK") || !second.contains("検証OK"))
                return "模擬切替未準備: 第1=" + first + " / 第2=" + second;
            CompoundTag slots = data.getCompound("awakened_dual_power_codec_slots_v1");
            String id1 = data.getString("awakened_dual_power_primary_id");
            String id2 = data.getString("awakened_dual_power_second");
            if (id1.isBlank() || id2.isBlank() || id1.equals(id2))
                return "模擬切替中止: 能力IDが未設定または重複しています";
            // Decode each saved slot twice to model 1 -> 2 -> 1 without ever attaching it.
            for (int slot : new int[] {1, 2, 1}) {
                CompoundTag snapshot = slots.getCompound("slot" + slot);
                PowerData detachedPower = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE,
                        snapshot.get("power")).result().orElseThrow();
                SkillData detachedSkill = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE,
                        snapshot.get("skill")).result().orElseThrow();
                String expected = slot == 1 ? id1 : id2;
                if (!expected.equals(detachedPower.powerId()))
                    return "模擬切替中止: スロット" + slot + "の能力IDが不一致";
                Tag power = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                        detachedPower).result().orElseThrow();
                Tag skill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                        detachedSkill).result().orElseThrow();
                if (!power.equals(snapshot.get("power")) || !skill.equals(snapshot.get("skill")))
                    return "模擬切替中止: スロット" + slot + "の再エンコードが不一致";
            }
            Tag afterPower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag afterSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            if (!beforePower.equals(afterPower) || !beforeSkill.equals(afterSkill)
                    || !beforeData.equals(data))
                return "模擬切替NG: 検査中にライブデータが変化しました";
            return "独立データの1→2→1模擬切替OK。ライブデータ変更なし。実際の能力切替は安全ロック中";
        } catch (Exception ex) {
            return "模擬切替失敗: " + ex.getClass().getSimpleName() + "（実際の切替なし）";
        }
    }

    /** Compares a saved slot to live data without changing either side. Useful before any real restore. */
    public static String compareLiveSlot(ServerPlayer player, int slot) {
        if (slot != 1 && slot != 2) return "不正なスロット番号";
        try {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot"))
                return "比較中止: 未完了の復旧記録があります";
            String checked = verifySlot(player, slot);
            if (!checked.contains("検証OK")) return "比較中止: " + checked;
            CompoundTag persistentBefore = data.copy();
            Tag livePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag liveSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            CompoundTag saved = data.getCompound("awakened_dual_power_codec_slots_v1")
                    .getCompound("slot" + slot);
            boolean powerSame = livePower.equals(saved.get("power"));
            boolean skillSame = liveSkill.equals(saved.get("skill"));
            String liveId = AwakenedAttachments.get(player).powerId();
            String savedId = saved.getString("power_id");
            Tag afterPower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag afterSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            if (!persistentBefore.equals(data) || !livePower.equals(afterPower) || !liveSkill.equals(afterSkill))
                return "比較NG: 読み取り中にライブデータが変化しました（切替禁止）";
            return "スロット" + slot + "比較: 現在ID=" + liveId + " / 保存ID=" + savedId
                    + " / PowerData=" + (powerSame ? "一致" : "差異あり")
                    + " / SkillData=" + (skillSame ? "一致" : "差異あり")
                    + "（読み取り専用・差異は育成によっても発生します）";
        } catch (Exception ex) {
            return "スロット比較失敗: " + ex.getClass().getSimpleName() + "（変更なし）";
        }
    }


    /** Strict read-only preflight for future attachment restoration. A PASS is NOT authorization to mutate. */
    public static String restorePreflight(ServerPlayer player) {
        try {
            CompoundTag data = player.getPersistentData();
            CompoundTag beforeData = data.copy();
            Tag beforePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag beforeSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            String problem = null;
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot")) {
                problem = "未完了の復旧記録があります";
            } else {
                String first = verifySlot(player, 1);
                String second = verifySlot(player, 2);
                if (!first.contains("検証OK") || !second.contains("検証OK")) {
                    problem = "保存スロットが未準備: 第1=" + first + " / 第2=" + second;
                } else {
                    String id1 = data.getString("awakened_dual_power_primary_id");
                    String id2 = data.getString("awakened_dual_power_second");
                    String liveId = AwakenedAttachments.get(player).powerId();
                    if (id1.isBlank() || id2.isBlank() || id1.equals(id2)) {
                        problem = "能力IDが空または重複しています";
                    } else if (!liveId.equals(id1) && !liveId.equals(id2)) {
                        problem = "現在の能力IDがどちらのスロットにも一致しません";
                    } else {
                        int active = liveId.equals(id1) ? 1 : 2;
                        CompoundTag activeSaved = data.getCompound("awakened_dual_power_codec_slots_v1")
                                .getCompound("slot" + active);
                        if (!beforePower.equals(activeSaved.get("power"))
                                || !beforeSkill.equals(activeSaved.get("skill"))) {
                            problem = "現在の能力データとスロット" + active
                                    + "の保存内容に差があります。育成後は再保存が必要です";
                        }
                    }
                }
            }
            Tag afterPower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag afterSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            if (!beforeData.equals(data) || !beforePower.equals(afterPower) || !beforeSkill.equals(afterSkill))
                return "復元事前診断NG: 検査中にデータが変化しました（切り替え禁止）";
            return problem == null
                    ? "復元事前診断PASS: 2スロット・能力ID・現在の保存内容が一致。ただし本体への適用安全性は未検証、切り替えは禁止"
                    : "復元事前診断NG: " + problem + "（データ変更なし）";
        } catch (Exception ex) {
            return "復元事前診断失敗: " + ex.getClass().getSimpleName() + "（切り替え禁止）";
        }
    }

}
