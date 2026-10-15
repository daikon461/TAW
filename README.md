# Awakened Dual Power v0.15 — integrity inspection

Minecraft 1.21.1 / NeoForge 21.1.219+ / Awakened 1.19.1 / Java 21.

## Added
`/dualpower audit` inspects addon-owned slot snapshots and IDs **without changing any data**.
Reports mismatched IDs, duplicate powers, invalid snapshot schemas, and negative/non-finite XP.
This is a diagnostic tool, **not** a guarantee of safe switching or recovery.

All previous commands remain. Real switching, Alt+V, skill restoration, and the
1.13.3-style GUI are still **not implemented**. Do not install in a hardcore
production world. Verify in a disposable test world.

## Build
Replace files in the existing GitHub repository, including `.github` and `libs`.
Run the Actions workflow and download the resulting JAR. NeoForge compilation
and in-game behavior are **not verified in this environment**.

## Test
Run `/dualpower slots save1`, `/dualpower slots show`, `/dualpower audit`.
Reconnect and run `/dualpower audit` again. Check the first power is unchanged.
