# v0.27 変更内容

- Awakened 1.19.1 に存在する公開API `ExoFlight.clear(ServerPlayer)`、`ProneFlight.set(ServerPlayer, false)`、`TempFlight.revoke(ServerPlayer, false)` を切り替え直前に呼び出すよう追加。
- `Scaling.clear(ServerPlayer)` は引き続き使用。
- クリエイティブ・スペクテイターの飛行権限は `TempFlight.revoke` 側が保護する。
- 切り替え処理は管理者による実験モード有効化が必要。NeoForgeビルド・ゲーム内挙動は未検証。

**制限:** 変身状態やその他の能力独自状態をすべて解除するものではありません。フライト状態は現在の完全復元スナップショット対象外なので、失敗時の全状態ロールバックも保証できません。バックアップ済みテストワールドのみで使用してください。
