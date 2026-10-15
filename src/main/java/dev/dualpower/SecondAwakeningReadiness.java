package dev.dualpower;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Non-mutating readiness report for the second awakening flow. */
public final class SecondAwakeningReadiness {
    private SecondAwakeningReadiness() {}

    public static String report(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        String current = AwakenedAttachments.get(player).powerId();
        String first = data.getString("awakened_dual_power_primary_id");
        String second = data.getString("awakened_dual_power_second");
        CompoundTag slots = data.getCompound("awakened_dual_power_codec_slots_v1");
        StringBuilder out = new StringBuilder("第二覚醒の安全診断（読取専用）: ");
        if (current.isBlank()) return out.append("第1能力が未覚醒です。先に通常の覚醒を行ってください").toString();
        if (first.isBlank()) return out.append("第1能力の登録がありません。既存データを確認してください").toString();
        if (!first.equals(current)) return out.append("現在の能力と第1能力の登録が一致しません。書き換えは行いません").toString();
        if (!slots.contains("slot1", Tag.TAG_COMPOUND)) return out.append("第1能力のCodecバックアップがありません。/dualpower codecslots save1 を実行してください").toString();
        String slot1Check = FullAwakenedSnapshot.verifySlot(player, 1);
        if (!slot1Check.contains("検証OK")) return out.append("第1能力の保存データが不正: ").append(slot1Check).toString();
        if (second.isBlank()) return out.append("第1能力のバックアップは正常。第2能力は未覚醒。結晶の使用は安全ロック中です").toString();
        if (second.equals(first)) return out.append("第1能力と第2能力のIDが同じです。切り替え不可").toString();
        if (!slots.contains("slot2", Tag.TAG_COMPOUND)) return out.append("第2能力のCodec保存データがありません。結晶の使用は安全ロック中です").toString();
        String slot2Check = FullAwakenedSnapshot.verifySlot(player, 2);
        if (!slot2Check.contains("検証OK")) return out.append("第2能力の保存データが不正: ").append(slot2Check).toString();
        return out.append("両スロットのCodec形式は正常。ただし内部データ復元未実装のため切り替えは安全ロック中です").toString();
    }
}
