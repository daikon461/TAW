# v0.36.1 修正

`codecslots save1/save2/verify1/verify2` の `sendSuccess` ラムダ式内で `getPlayerOrException()` を呼び出していた4箇所を修正。例外を投げる可能性のあるプレイヤー取得をラムダの外へ移動しました。ゲーム内機能はv0.36と同じで、安全ロックは維持します。NeoForgeビルド未検証。
