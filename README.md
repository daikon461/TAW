# Awakened Dual Power v0.19 — Alt+V input preview

Target: Minecraft 1.21.1 / NeoForge 21.1.219 / Awakened 1.19.1 / Java 21.

## New
- Client-only key mapping (V; hold Left/Right Alt and press V).
- Japanese and English localization of the key and preview message.
- Shows an action-bar confirmation when Alt+V is detected.
- Dedicated server never loads the client-only handler.
- Existing `/dualpower` checkpoint, audit, backup and readiness commands retained.

**Safety:** This version does NOT switch powers, restore skill data, clear
scaling/flight effects, or reproduce the 1.13.3 GUI. It sends no network
packets and does not modify Awakened data when Alt+V is pressed.

## GitHub Actions
Upload the complete extracted project, including `.github` and `libs`.
Run the existing build workflow and download the artifact.
The NeoForge build has NOT been run in this environment; source may need
API adjustments. Previous loader-independent tests can be run with Java 21.

## Test
1. Back up your world, use a separate non-hardcore test world.
2. Confirm `/dualpower status` still works.
3. Hold Alt and press V: action bar should show a preview confirmation.
4. Confirm Awakened power, level, XP and size are unchanged.
5. Verify ordinary V alone does not display the confirmation.

Note: If Awakened already binds V, change that binding in Controls to avoid
input conflicts. This implementation checks Alt as a modifier rather than
registering a native multi-key chord.
