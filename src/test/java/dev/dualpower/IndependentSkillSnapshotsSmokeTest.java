package dev.dualpower;
import java.util.Map;
import java.util.Properties;
public final class IndependentSkillSnapshotsSmokeTest {
    public static void main(String[] args) {
        var snap = new IndependentSkillSnapshots(Map.of("awakened:skill", 17),Map.of("awakened:skill", 42));
        var roundtrip = IndependentSkillSnapshots.decode(snap.encode());
        if (roundtrip.first().get("awakened:skill") != 17 || roundtrip.second().get("awakened:skill") != 42)
            throw new AssertionError("slot isolation failed");
        try { roundtrip.first().put("x",3); throw new AssertionError("mutable snapshot"); }
        catch (UnsupportedOperationException expected) { }
        Properties corrupted = snap.encode(); corrupted.setProperty("second.awakened:skill","-2");
        try { IndependentSkillSnapshots.decode(corrupted); throw new AssertionError("corrupt data accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PASS: independent snapshots, immutability, corrupt-data rejection");
    }
}
