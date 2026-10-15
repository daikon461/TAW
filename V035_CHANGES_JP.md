# v0.35: 能力別Codec保存スロット（読み取り専用）

- `/dualpower codecslots save1` / `save2`: 現在のAwakened能力IDと登録スロットIDが一致する場合だけPowerData/SkillDataのCodecスナップショットを保存。
- `/dualpower codecslots verify1` / `verify2`: 保存データをデコード・再エンコードして検証。
- 以前の同一スロットの保存データを1世代バックアップ。
- どのコマンドもAwakened本体の能力を変更しない。Alt+Vの安全ロックは維持。
- スロット2を保存するには、Awakened本体側で実際に第2能力を保持している必要がある。IDの登録だけでは保存できない。
- Codecだけでは他のアタッチメントやワールド状態の完全保存を保証しない。
- NeoForgeビルド・ゲーム内テスト未検証。
