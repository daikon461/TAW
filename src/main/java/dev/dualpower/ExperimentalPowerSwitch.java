package dev.dualpower;

import net.minecraft.server.level.ServerPlayer;

/** Fail-closed entry point: never mutate Awakened until a full-state transaction exists. */
public final class ExperimentalPowerSwitch {
    private ExperimentalPowerSwitch() {}
    public static final String ENABLE_FLAG = "awakened_dual_power_experimental_enabled";

    public static String execute(ServerPlayer player) {
        if (player.getPersistentData().getBoolean("awakened_dual_power_transaction_pending")
                || player.getPersistentData().contains("awakened_dual_power_recovery_snapshot")) {
            return "未完了の復旧記録があるため、切り替えを停止しました。バックアップを確認してください";
        }
        return "安全ロック中: Awakenedの全内部データを保存・復元できるまで能力変更は行いません";
    }
}
