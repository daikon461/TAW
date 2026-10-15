# v0.38 第二覚醒の結晶・安全診断連携

結晶を右クリックすると読み取り専用の覚醒準備診断を表示します。実際の第二覚醒と切り替えは安全ロック中です。

# Awakened Dual Power v0.30 — Recovery Guard

See V030_CHANGES_JP.md. This is **NOT a production release**.

> v0.29: 再起動をまたぐ5秒クールダウンと復元結果の通知を改善。詳細は V029_CHANGES_JP.md。

# Awakened Dual Power v0.28 — Persistent Cooldown

# Awakened Dual Power v0.27 — Flight Cleanup Experimental

Target: Minecraft 1.21.1 / NeoForge 21.1.x / Awakened 1.19.1 / Java 21.

## v0.27
See V027_CHANGES_JP.md for flight cleanup API additions.

## Changes
- Stores an addon-owned `awakened_dual_power_recovery_snapshot` BEFORE attempting to mutate Awakened data.
- Preserves the pending marker and snapshot if rollback fails; blocks further switching.
- Clears recovery metadata after successful switch or verified rollback.
- Fixes preview-mode cooldown consumption: preview checks no longer start a cooldown.

## Known limitations (important)
- Experimental power switching is NOT safe for production or hardcore worlds.
- Full skill-tree state, active effects, transformations, flight, client synchronization and the old 1.13.3 menu are NOT implemented/verified.
- This recovery snapshot is **not an automatic crash recovery system**. Never manually delete a pending marker without a backup.
- NeoForge Gradle build and in-game behavior are not verified in this environment.

## Build
Upload the entire source to the existing GitHub repository including `.github` and `libs` and run Actions. Test in a COPY of a normal world only. Keep experimental mode disabled unless intentionally testing.


## v0.26: verified rollback
- After power assignment, verify power ID, level, XP and every Stat allocation before committing the source slot.
- On exception, retain the pending transaction and recovery snapshot unless the full source state matches after rollback.
- This does NOT capture all Awakened internal power state, transformation or flight; experimental mode remains off by default.
- NeoForge build and in-game behavior have not been verified in this environment.

## v0.28
See V028_CHANGES_JP.md. Cooldown is stored per player instead of only in server process memory.


## v0.31 safety interlock
Power mutation is blocked even with experimental enable until full Awakened internal state serialization is implemented.

## v0.32 注意
安全優先で能力変更コードを削除しました。Alt+Vは検査のみです。v1.0.0完成版ではありません。


## v0.36 第二覚醒の結晶
レシピとアイテムを追加。中心はネザースター。**使用しても覚醒せず消費もしない安全ロック版**。詳細は V036_CHANGES_JP.md。

## v0.37 第二覚醒の安全診断

`/dualpower awakeningcheck` で第1能力の登録とCodecバックアップ、第二能力の保存状況を確認できます。読取専用で、覚醒や能力変更は行いません。結晶は引き続き安全ロックされています。

## v0.39
`/dualpower restorerehearsal`: 読み取り専用の復元予行演習。実際の復元や能力変更は行いません。

## v0.40
前世代のCodecバックアップを `/dualpower codecbackup verify1` / `verify2` で非破壊検証できます。第二覚醒・能力切り替えは引き続き安全ロック中です。
