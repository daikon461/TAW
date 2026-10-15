package dev.dualpower;

/**
 * Adapter boundary for future NeoForge integration.
 * Implementations must execute on the logical server thread.
 * Do not invoke with an unverified Awakened adapter.
 */
public final class SafeSwitchCoordinator {
    private SafeSwitchCoordinator() {}

    public interface Adapter {
        /** Opaque, complete backup of player state, including skills and power effects. */
        Object snapshot();
        /** Throws if switching would be unsafe (e.g. power-specific effect not supported). */
        void validateSwitch(String from, String to);
        /** Remove persistent effects of the old power, including size modifiers. */
        void clearEffects(String from);
        /** Assign the new Awakened power without wiping existing mastery. */
        void assignPower(String to);
        /** Restore only the selected power's separately stored skill allocation. */
        void restoreSkills(String to);
        /** Sync Awakened state to clients. */
        void sync();
        /** Restore full snapshot on failure. Must be safe after partial mutation. */
        void rollback(Object snapshot);
    }

    public static DualPowerState switchPower(DualPowerState state, long tick, Adapter adapter) {
        if (!state.canSwitch(tick, PowerSwitchPlan.COOLDOWN_TICKS))
            throw new IllegalStateException("Second power absent or cooldown active");
        String from = state.activePower(), to = state.inactivePower();
        if (from == null || to == null) throw new IllegalStateException("Missing power");
        adapter.validateSwitch(from, to);
        Object backup = adapter.snapshot();
        try {
            adapter.clearEffects(from);
            adapter.assignPower(to);
            adapter.restoreSkills(to);
            adapter.sync();
            return state.switchAt(tick, PowerSwitchPlan.COOLDOWN_TICKS);
        } catch (RuntimeException | Error error) {
            try { adapter.rollback(backup); }
            catch (RuntimeException | Error rollbackError) { error.addSuppressed(rollbackError); }
            throw error;
        }
    }
}
