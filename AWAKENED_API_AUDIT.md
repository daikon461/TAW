# Awakened 1.13.3 binary API audit (2026-10-15)

Source: user-supplied `awakened-1.13.3+1.21.1.jar` inspected with `javap -public`.

- `PowerData.powerId(): String`
- `PowerData.assignPower(String, boolean): void`
- `PowerData.currentMastery(): MasteryData`
- `AwakenedAttachments.get(Player): PowerData`
- `AwakenedAttachments.skill(Player): SkillData`
- `SkillData.points(Stat): int`
- `SkillData.setPoints(Stat, int): void`
- `SkillData.clearAllocations(): void`
- `Scaling.clear(ServerPlayer): void`

Metadata: `awakened` version `1.13.3`, NeoForge `[21.1.219,)`, Minecraft `[1.21,1.21.1]`.

**Unknown / not yet implemented:** effects beyond Scaling, skill/stat scope, exact behavior of `assignPower`, player-data persistence, safe rollback on partial failure, game networking and key handling. Method presence is not proof of safety.
