package dev.dualpower;
/** Names and descriptors inspected in Awakened 1.13.3 (Java bytecode via javap).
 * These are documentation constants, not a linked or working NeoForge adapter.
 */
public final class AwakenedApiContract {
    private AwakenedApiContract() {}
    public static final String POWER_DATA = "com.awakenedmod.awakened.core.data.PowerData";
    public static final String ATTACHMENTS = "com.awakenedmod.awakened.core.data.AwakenedAttachments";
    public static final String SKILL_DATA = "com.awakenedmod.awakened.core.data.SkillData";
    public static final String SCALING = "com.awakenedmod.awakened.content.scaler.Scaling";
    public static final String ASSIGN_POWER = "assignPower(java.lang.String, boolean)";
    public static final String CLEAR_SCALE = "clear(net.minecraft.server.level.ServerPlayer)";
}
