# v0.31 安全ロック

Awakened 1.19.1 の PowerData には、従来のスナップショットで保存していない stamina、mastery、vars、strVars、banked などの内部状態が存在します。

データ破損を防ぐため、管理者が experimental enable を実行しても、実際の assignPower は呼び出しません。Alt+V の入力、サーバー通信、保存データの検査は引き続き利用できます。

この版は完成版ではありません。完全な内部データのトランザクション保存・復元、NeoForge ビルド、ゲーム内試験が未完了です。
