# v0.49 第2能力候補の保存（実験版）

`/dualpower secondcandidate_save <能力ID>` を追加。第1スロット検証後、独立した新規PowerData・SkillDataを生成し、第2スロットに保存する。保存後検証失敗時は変更した登録キーをロールバックする。既存の第2スロットは上書きしない。実在する能力IDかどうかは未検証であり、必ずコピーしたテストワールドで実施すること。実際の第二覚醒、結晶消費、Alt+V切り替えは無効のまま。`/dualpower codecslots verify2` で確認可能。
