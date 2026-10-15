# Binary API audit: Awakened 1.19.1
Inspected provided `awakened-1.19.1+1.21.1.jar` with `javap` and its `META-INF/neoforge.mods.toml`.
- `com.awakenedmod.awakened.core.data.PowerData.powerId(): String`
- `PowerData.assignPower(String, boolean): void`
- `AwakenedAttachments.get(Player): PowerData`
- `AwakenedAttachments.skill(Player): SkillData`
- `com.awakenedmod.awakened.content.scaler.Scaling.clear(ServerPlayer): void`
- mod ID: `awakened`, version: `1.19.1`, minimum NeoForge: `21.1.219`.
Method presence does not establish state semantics or safe power switching.
