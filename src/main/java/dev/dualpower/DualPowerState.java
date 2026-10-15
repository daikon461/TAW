package dev.dualpower;

/**
 * Loader-independent, immutable two-slot state.
 * This class does not change Awakened data; it is the safe foundation
 * for a future NeoForge server-side adapter.
 */
public record DualPowerState(String firstId, String secondId, int activeSlot, long lastSwitchTick) {
    public DualPowerState {
        if (activeSlot != 0 && activeSlot != 1) throw new IllegalArgumentException("activeSlot");
        firstId = normalize(firstId);
        secondId = normalize(secondId);
        if (firstId != null && firstId.equals(secondId)) throw new IllegalArgumentException("Duplicate powers");
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) return null;
        return id.trim();
    }

    public String activePower() { return activeSlot == 0 ? firstId : secondId; }
    public String inactivePower() { return activeSlot == 0 ? secondId : firstId; }

    public boolean canSwitch(long currentTick, int cooldownTicks) {
        return inactivePower() != null && currentTick >= lastSwitchTick
            && currentTick - lastSwitchTick >= cooldownTicks;
    }

    public DualPowerState switchAt(long tick, int cooldownTicks) {
        if (!canSwitch(tick, cooldownTicks)) throw new IllegalStateException("Switch unavailable");
        return new DualPowerState(firstId, secondId, 1 - activeSlot, tick);
    }
}
