package dev.dualpower;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Reports the exact blocker to a second awakening without mutating player data. */
public final class SecondAwakeningPath {
    private SecondAwakeningPath() {}

    public static String report(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        String live = AwakenedAttachments.get(player).powerId();
        String first = data.getString("awakened_dual_power_primary_id");
        String second = data.getString("awakened_dual_power_second");
        CompoundTag slots = data.getCompound("awakened_dual_power_codec_slots_v1");
        if (data.getBoolean("awakened_dual_power_transaction_pending")
                || data.contains("awakened_dual_power_recovery_snapshot"))
            return "第二覚醒準備: 停止。未完了の復旧記録があります。保存データを保護してください";
        if (live.isBlank()) return "第二覚醒準備: 通常の第1覚醒を先に行ってください";
        if (first.isBlank()) return "第二覚醒準備: 第1能力の登録がありません。/dualpower awakeningcheck で確認してください";
        if (!live.equals(first)) return "第二覚醒準備: 現在の能力と第1能力の登録が異なります。上書き禁止";
        if (!slots.contains("slot1", Tag.TAG_COMPOUND))
            return "第二覚醒準備: 第1能力の保存が必要です。/dualpower codecslots save1 を実行してください";
        String check1 = FullAwakenedSnapshot.verifySlot(player, 1);
        if (!check1.contains("検証OK")) return "第二覚醒準備: 第1能力の保存検証NG: " + check1;
        if (second.isBlank())
            return "第二覚醒準備: 第1能力は保存済み。第2能力は未取得です。Awakenedの独立した覚醒生成・適用処理は未実装。結晶は消費されません";
        if (second.equals(first)) return "第二覚醒準備: 第2能力IDが第1能力と重複しています。変更禁止";
        if (!slots.contains("slot2", Tag.TAG_COMPOUND))
            return "第二覚醒準備: 第2能力IDは登録済みですが実体のCodecデータがありません。IDだけでは能力は生成されません";
        String check2 = FullAwakenedSnapshot.verifySlot(player, 2);
        if (!check2.contains("検証OK")) return "第二覚醒準備: 第2能力の保存検証NG: " + check2;
        return "第二覚醒準備: 両スロットの保存検証OK。ただし安全な実データ適用は未実装。切り替えはロック中";
    }
}
