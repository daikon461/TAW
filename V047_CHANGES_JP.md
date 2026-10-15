# v0.47 Awakenedアタッチメント保存範囲の監査

`/dualpower attachmentaudit` を追加。Awakened 1.19.1 の PowerData、SkillData、ProgressData、QuestData、RelicData の各Codecについて、読み込み→再エンコードの一致を読み取り専用で確認します。

**重要:** 既存の能力スロットはPowerDataとSkillDataしか保存していません。Progress、Quest、Relicのデータはプレイヤー全体で共有すべきか能力別に分けるべきか設計が未確定です。また、Awakenedには外見・装備・一時状態等の追加アタッチメントが存在します。これらを網羅するまで安全な切り替えは保証できません。

第2能力の生成・結晶の消費・実際のAlt+V切り替えはロックしたままです。ビルドとゲーム内テストは未実施です。
