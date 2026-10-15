# v0.44 変更内容

- `/dualpower slotcompare 1` と `/dualpower slotcompare 2` を追加。
- 現在のPowerData/SkillDataと各Codec保存スロットの一致・差異を読み取り専用で比較。
- 検査前後のライブCodecデータと永続データが変化しないことを再確認。
- 未完了トランザクションやスロット不整合がある場合は比較を停止。
- 実際の能力復元、第二覚醒、Alt+Vのロックは維持。
- ビルド・ゲーム内テストは未実施。
