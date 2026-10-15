# v0.50 登録能力IDの厳格検証

- Awakened 1.19.1の `PowerRegistry.get(id)` を利用して、存在しない能力IDを候補生成・スロット2保存の両方で拒否。
- 第1能力への書き込み、実際の能力切り替え、結晶消費は引き続き無効。
- `/dualpower secondcandidate <ID>` と `/dualpower secondcandidate_save <ID>` をテストワールドで確認。
- 第2スロットを既に保存済みなら上書きしない。
- 実際の第二覚醒や復元の安全性はまだ未検証。
