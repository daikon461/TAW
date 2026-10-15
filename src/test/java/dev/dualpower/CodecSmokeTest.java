package dev.dualpower;

public class CodecSmokeTest {
    public static void main(String[] args) {
        DualPowerState original = new DualPowerState("fire","ice",1,200L);
        DualPowerState decoded = DualPowerSaveCodec.decode(DualPowerSaveCodec.encode(original));
        if (!original.equals(decoded)) throw new AssertionError("Save round trip");
        try {
            var bad = DualPowerSaveCodec.encode(original);
            bad.setProperty("version","999");
            DualPowerSaveCodec.decode(bad);
            throw new AssertionError("Invalid version accepted");
        } catch (IllegalArgumentException expected) {}
        System.out.println("PASS: save roundtrip, schema version validation");
    }
}
