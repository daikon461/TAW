package dev.dualpower;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-only service boundary. Call on the logical server thread.
 * In-memory states MUST be loaded from / saved to a future NeoForge attachment.
 * Not yet wired to the game.
 */
public final class ServerSwitchService {
    private final Map<UUID, DualPowerState> states = new HashMap<>();

    public void load(UUID player, DualPowerState state) {
        if (player == null || state == null) throw new IllegalArgumentException();
        states.put(player, state);
    }

    public DualPowerState get(UUID player) { return states.get(player); }

    public DualPowerState requestSwitch(UUID player, long gameTick,
                                        SafeSwitchCoordinator.Adapter adapter) {
        DualPowerState before = states.get(player);
        if (before == null) throw new IllegalStateException("No registered power state");
        DualPowerState after = SafeSwitchCoordinator.switchPower(before, gameTick, adapter);
        states.put(player, after); // commit only after successful adapter switch
        return after;
    }

    /** Clear cache on disconnect; persistence must be handled by NeoForge. */
    public void unload(UUID player) { states.remove(player); }
}
