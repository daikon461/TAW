# v0.33 全データCodecスナップショット検証

- Awakened 1.19.1 の `PowerData.MAP_CODEC` と `SkillData.MAP_CODEC` を使用して、現在の能力・スキルデータを NBT にシリアライズします。
- 直後にCodecでデコードして、能力ID・熟練度レベル・経験値の往復一致を検証します。
- 成功した場合に限り、プレイヤーのアドオン専用NBT `awakened_dual_power_full_snapshot_audit_v1` に記録します。
- **安全ロックは継続**。このスナップショットをAwakenedに適用したり、能力を切り替えたりはしません。
- 対象はPowerDataとSkillDataのみです。Awakenedの追加アタッチメントや能力のワールド側状態は含みません。
- 実際のNeoForgeビルド・ゲーム内テストは未検証です。
