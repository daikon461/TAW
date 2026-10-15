package dev.dualpower;

/** Pure planning step. No player mutation is performed here. */
public final class PowerSwitchPlan {
    public static final int COOLDOWN_TICKS = 100; // five seconds at 20 TPS
    private PowerSwitchPlan() {}

    public static DualPowerState plan(DualPowerState state, long gameTick) {
        return state.switchAt(gameTick, COOLDOWN_TICKS);
    }
}
