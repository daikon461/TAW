package dev.dualpower;

/**
 * Server-side policy for registering a second power.
 * The future NeoForge adapter must verify that the unlock requirement was met.
 * This class never writes directly to Awakened's player data.
 */
public final class SecondAwakeningService {
    private SecondAwakeningService() {}

    public interface UnlockAuthority {
        boolean isEligible(String playerId);
        boolean isKnownPower(String powerId);
    }

    private static boolean isValidPowerId(String id) {
        try { PowerIdValidator.requireValid(id); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    public static DualPowerState registerSecond(
            String playerId, DualPowerState current, String newPower,
            UnlockAuthority authority) {
        if (playerId == null || playerId.isBlank() || current == null ||
            authority == null || newPower == null || newPower.isBlank())
            throw new IllegalArgumentException("Missing registration input");
        if (current.firstId() == null) throw new IllegalStateException("First awakening required");
        if (current.secondId() != null) throw new IllegalStateException("Second awakening already registered");
        if (newPower.equals(current.firstId())) throw new IllegalArgumentException("Duplicate power");
        if (!authority.isKnownPower(PowerIdValidator.requireValid(newPower)))
            throw new IllegalArgumentException("Unknown power ID");
        if (!authority.isEligible(playerId)) throw new IllegalStateException("Second awakening locked");
        return new DualPowerState(current.firstId(), newPower, current.activeSlot(), current.lastSwitchTick());
    }
}
