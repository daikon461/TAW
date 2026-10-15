package dev.dualpower;

import java.util.Properties;

/**
 * Pure codec for state persistence. The future NeoForge attachment layer
 * is responsible for writing/reading the Properties and data migration.
 */
public final class DualPowerSaveCodec {
    private DualPowerSaveCodec() {}
    public static Properties encode(DualPowerState state) {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("first", state.firstId() == null ? "" : state.firstId());
        p.setProperty("second", state.secondId() == null ? "" : state.secondId());
        p.setProperty("active", Integer.toString(state.activeSlot()));
        p.setProperty("lastSwitchTick", Long.toString(state.lastSwitchTick()));
        return p;
    }
    public static DualPowerState decode(Properties p) {
        if (!"1".equals(p.getProperty("version")))
            throw new IllegalArgumentException("Unsupported save version");
        int active = Integer.parseInt(p.getProperty("active"));
        long tick = Long.parseLong(p.getProperty("lastSwitchTick"));
        return new DualPowerState(p.getProperty("first"), p.getProperty("second"), active, tick);
    }
}
