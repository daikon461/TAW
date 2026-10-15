# v0.34 安全な保存データ検証

- `/dualpower fullsnapshot save`: PowerDataとSkillDataをCodecで保存し、往復デコード検証。
- `/dualpower fullsnapshot verify`: 保存したCodecデータを再デコードし、現在のAwakenedデータと読み取り専用で比較。
- 変化の検出はエラーを意味しません。通常の熟練度上昇や能力使用でも差分が発生します。
- 能力切り替えと自動復元は安全ロックのままです。
- Codec対象外のアタッチメントやワールド状態は含みません。
- NeoForgeビルド・実機テスト未検証。
