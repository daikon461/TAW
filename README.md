# Awakened Dual Power v0.11 — safe snapshot prototype

Target: Minecraft 1.21.1, NeoForge 21.1.219, Awakened 1.19.1, Java 21.

New: immutable, validated per-slot skill snapshots with versioned codec, roundtrip and corruption tests. This is a **standalone storage codec**, not yet wired to player NBT/attachments; no actual in-game skill persistence for two slots yet.

Menu audit: see `GUI_COMPATIBILITY_1133_TO_1191.md`. The requested 1.13.3 menu has **not yet been restored**; v0.11 intentionally leaves 1.19.1's menu untouched.

Existing `/dualpower status` and metadata-only `second set/clear` commands remain. **No live ability switching** is enabled. Test only in a backed-up world.

Build using `.github/workflows/build.yml`; `libs/awakened-1.19.1+1.21.1.jar` must be uploaded. Gradle build was not run in this environment. Pure Java tests were run with javac/java 21.
