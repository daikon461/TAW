package dev.dualpower;

public final class SecondAwakeningSmokeTest {
    public static void main(String[] args) {
        var allowed = new SecondAwakeningService.UnlockAuthority() {
            public boolean isEligible(String playerId) { return true; }
            public boolean isKnownPower(String powerId) { return "awakened:ice".equals(powerId); }
        };
        var denied = new SecondAwakeningService.UnlockAuthority() {
            public boolean isEligible(String playerId) { return false; }
            public boolean isKnownPower(String powerId) { return true; }
        };
        DualPowerState initial = new DualPowerState("awakened:fire", null, 0, 0);
        DualPowerState registered = SecondAwakeningService.registerSecond("player", initial, "awakened:ice", allowed);
        if (!"awakened:ice".equals(registered.secondId()) || initial.secondId()!=null)
            throw new AssertionError("registration should be immutable");
        assertRejected(() -> SecondAwakeningService.registerSecond("player", initial, "awakened:ice", denied));
        assertRejected(() -> SecondAwakeningService.registerSecond("player", initial, "awakened:fire", allowed));
        assertRejected(() -> SecondAwakeningService.registerSecond("player", registered, "awakened:ice", allowed));
        assertRejected(() -> SecondAwakeningService.registerSecond("player", initial, "awakened:unknown", allowed));
        System.out.println("PASS: registration, lock, duplicate, already-owned, unknown");
    }
    private static void assertRejected(Runnable task) {
        try { task.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException | IllegalStateException expected) {}
    }
}
