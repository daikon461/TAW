package dev.dualpower;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.SkillData;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Strict, read-only transaction gate. It does NOT authorize actual switching. */
public final class SwitchReadinessGate {
    private SwitchReadinessGate() {}

    public static String report(ServerPlayer player) {
        try {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot"))
                return "切替準備NG: 未完了の復旧記録があります";
            String primary = data.getString("awakened_dual_power_primary_id");
            String secondary = data.getString("awakened_dual_power_second");
            if (primary.isBlank() || secondary.isBlank() || primary.equals(secondary)
                    || PowerRegistry.get(primary) == null || PowerRegistry.get(secondary) == null)
                return "切替準備NG: 2つの異なる登録済み能力IDが必要です";
            if (!primary.equals(AwakenedAttachments.get(player).powerId()))
                return "切替準備NG: 現在の能力が第1能力ではありません";
            if (!FullAwakenedSnapshot.verifySlot(player, 1).contains("検証OK")
                    || !FullAwakenedSnapshot.verifySlot(player, 2).contains("検証OK"))
                return "切替準備NG: 保存スロットのCodec検証に失敗しました";
            CompoundTag slots = data.getCompound("awakened_dual_power_codec_slots_v1");
            CompoundTag first = slots.getCompound("slot1");
            CompoundTag second = slots.getCompound("slot2");
            Tag livePower = PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.get(player)).result().orElseThrow();
            Tag liveSkill = SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE,
                    AwakenedAttachments.skill(player)).result().orElseThrow();
            if (!livePower.equals(first.get("power")) || !liveSkill.equals(first.get("skill")))
                return "切替準備NG: 第1能力の保存データが古いです。/dualpower codecslots save1 で更新してください";
            PowerData p2 = PowerData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, second.get("power"))
                    .result().orElseThrow();
            SkillData s2 = SkillData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, second.get("skill"))
                    .result().orElseThrow();
            if (!secondary.equals(p2.powerId())
                    || !second.get("power").equals(PowerData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, p2).result().orElseThrow())
                    || !second.get("skill").equals(SkillData.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, s2).result().orElseThrow()))
                return "切替準備NG: 第2能力の復元データが一致しません";
            return "切替データ準備OK: 第1・第2能力の識別子とCodec保存データを照合しました。"
                    + "ただし実際の切替は未許可（効果・変身・飛行・全アタッチメントの復旧は未検証）";
        } catch (Exception ex) {
            return "切替準備NG: " + ex.getClass().getSimpleName() + "（能力変更なし）";
        }
    }
}
