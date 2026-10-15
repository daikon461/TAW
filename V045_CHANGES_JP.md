# v0.45 復元前の厳格な事前診断

- `/dualpower restorepreflight` を追加。
- 2スロットのCodec検証、能力ID重複、未完了復旧記録、現在の能力IDの登録、現在のCodecデータとアクティブスロット保存内容の完全一致を確認。
- 診断前後のPowerData、SkillData、永続NBTの不変性を確認。
- PASSは実際の復元安全性を証明しません。結晶とAlt+Vの安全ロックは継続。
- `build.gradle` の古いバージョン表記を0.45.0へ修正。
- ZIP整合性のみ検証。Gradleビルド・ゲーム内テスト未実施。
