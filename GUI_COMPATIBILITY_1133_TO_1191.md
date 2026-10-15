# Character menu compatibility audit — 1.13.3 → 1.19.1

Examined both user-provided JARs with `javap`.

- Both contain `com.awakenedmod.awakened.client.screen.CharacterScreen` extending `AwakenedScreen`.
- Both retain constructors `CharacterScreen()` and `CharacterScreen(int)` and mouse/key/render methods.
- 1.13.3 exposes `SETTINGS_TAB`, `TEAM_TAB`, `RELICS_TAB`.
- 1.19.1 exposes `POWER_TAB`, `STATS_TAB`, `TRIALS_TAB`, `RELICS_TAB`, `RECIPES_TAB`, `SETTINGS_TAB`, `TEAM_TAB`.
- The old screen class cannot safely replace the newer one without further inspection of its internal calls and data model.

**v0.11 does not replace or modify the 1.19.1 menu.** This avoids risking a crash or overwriting the user's preferred settings. Next step is a client-only compatibility UI (old navigation layout backed by 1.19.1 API), after examining method bytecode and texture assets.
