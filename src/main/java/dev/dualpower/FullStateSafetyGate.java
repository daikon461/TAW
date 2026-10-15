package dev.dualpower;
/** Fail-closed until full PowerData and SkillData transaction support is verified. */
public final class FullStateSafetyGate {
    private FullStateSafetyGate() {}
    public static boolean canSwitch() { return false; }
}
