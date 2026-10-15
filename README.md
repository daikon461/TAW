# Awakened Dual Power v0.9 — registration-only test build

Target: Minecraft 1.21.1, NeoForge 21.1.219, Awakened 1.13.3, Java 21.

## Added
- `/dualpower status` shows your registered second ability ID.
- OP-only `/dualpower second set <power_id>` saves a string to the player persistent NBT.
- OP-only `/dualpower second clear` removes the registration.
- The registration does NOT verify that the power exists in Awakened.
- This is NOT a real second awakening and does not switch any powers.

## Important limitations
- Player persistent NBT survives normal save/reload, but death/clone transfer is **not implemented or tested**.
- No actual Awakened API adapter, no Alt+V, no effect cleanup, no ability/skill swapping.
- This project has NOT been built against NeoForge dependencies in this environment.
- Test in a **separate test world**; back up your saves.

## Build
Extract and upload all contents including `.github` to GitHub. Run Actions workflow
`Build NeoForge Bootstrap`. The Gradle build may expose API issues; report the log.
