# Awakened Dual Power v0.10 — Awakened 1.19.1 compatibility check

Targets Minecraft 1.21.1, NeoForge 21.1.219+, Java 21, Awakened **1.19.1**.

## Changes
- Exact Awakened 1.19.1 dependency range in `neoforge.mods.toml`.
- `compileOnly` dependency on the supplied Awakened JAR in `libs/`. This binary is not embedded in the addon JAR.
- `/dualpower status` reads the primary Awakened power ID without changing it, and shows separately stored second-power metadata.
- Administrative `/dualpower second set <id>` and `clear` still manage *metadata only*; they do not grant a power or validate the power ID against Awakened's registry.

## Safety
No Alt+V, actual power switching, skill restoration, effect cleanup, or migration is enabled.
Use a copy of a test world; back up the original first.

## Build
Upload all files to GitHub, including `.github/workflows/build.yml` and `libs/`.
Run the GitHub Actions workflow. Build is **not verified here**.
In game run `/dualpower status` and check that the first ability shown matches the existing Awakened ability.
Do not run `second set` with arbitrary IDs on a production world.
