# SmartERP ロードマップ

> **ビジョン：** SmartERP は、オートメーション・財務核算・資金管理・ポートフォリオ分析を統合したエンタープライズ経営プラットフォームへと継続的に進化しています。

---

## モジュール構成

```
SmartERP
├── OA（v1.0）✅
├── HR（予定）
├── Finance
│   ├── v2.0 Accounting（財務核算）
│   ├── v3.0 Treasury（資金管理）
│   └── v4.0 Portfolio（ポートフォリオ管理）
├── v5.0 Analytics（BI 分析）
└── v6.0 AI（AI アシスタント）
```

---

## v1.0 OA モジュール ✅ 完了

**目的：**「テンプレート設定 + フローエンジン」アーキテクチャの検証と、ERP システムの承認・権限基盤の構築。

- JWT 認証 + BCrypt パスワード暗号化 + ロール区別
- 多段階承認エンジン：条件分岐（SpEL）/ 並行承認（会簽・或簽）/ タイムアウト自動エスカレーション
- 休暇管理 + 経費精算
- 複式簿記エンジン（借方・貸方自動生成、会計恒等式の保証）
- BigDecimal 精度制御（DECIMAL(19,2)、double/float 不使用）
- 楽観ロック（@Version）による同時承認の競合防止
- 赤字消し戻し（借方・貸方を入れ替えた反対仕訳の生成）
- 監査ログ（追加のみ、更新・削除不可）
- 状態マシン制約：DRAFT → PENDING → APPROVED → POSTED
- テーブル 12枚 / ユニットテスト 41件 / コード約 8,500 行

---

## v2.0 Finance — Accounting（財務核算）

**目的：** 企業の財務核算ニーズに対応し、複式簿記と証票管理を実現する。

- Chart of Accounts（勘定科目体系）
- Journal Entry（仕訳管理）
- Double-Entry Bookkeeping（複式簿記）
- Reversal（赤字消し戻し）
- Cost Center（コストセンター）
- Profit Center（プロフィットセンター）
- Fiscal Period（会計期間 / 月次決算）
- Audit Trail（監査ログ）
- Financial Dashboard

---

## v3.0 Finance — Treasury（資金管理）

**目的：** 企業のキャッシュフロー、銀行口座、資金振替を統合管理し、資金の分散・非透明性を解消する。

- Corporate Account（法人アカウント）
- Bank Account Management（銀行口座管理）
- Cash Position（現金残高）
- Accounts Receivable / Payable（売掛金 / 買掛金）
- Cash Flow Management（キャッシュフロー管理）
- Intercompany Transfer（社内資金振替）
- Bank Reconciliation（銀行照合）
- Cash Flow Forecasting（資金フロー予測）
- Treasury Investment（資金運用 — マネーマーケットファンド等の短期運用）
- Treasury Dashboard

---

## v4.0 Finance — Portfolio（ポートフォリオ管理）

**目的：** 企業の遊休資金の運用を支援し、収益率・リスク分析・資産配分能力を提供する。

- Investment Portfolio（ETF / 株式 / 傾券[予定] / 現金）
- Holdings Management（保有管理）
- Trade & Settlement（取引記録・決済）
- Dividend Management（配当管理）
- Asset Allocation（資産配分）
- Portfolio Performance（ポートフォリオパフォーマンス：収益率 / 年間収益率）
- Time-Weighted Return / Money-Weighted Return
- Max Drawdown（最大ドローダウン）
- Sharpe Ratio（シャープレシオ）
- Volatility Analysis（ボラティリティ分析）
- Risk Analysis（リスク分析）
- Portfolio Dashboard

**デモデータ：** QQQM / VOO / VTI

---

## v5.0 Analytics（BI 分析）

**目的：** OA・財務・資金・投資データを一元集約し、ビジュアル経営インサイトを提供する。

- Business Intelligence Dashboard
- Financial Statements（財務諸表）
- Profitability Analysis（収益性分析）
- Budget vs Actual（予算実績対比）
- Investment Return Trends（投資収益トレンド）
- Cash Flow Forecast（キャッシュフロー予測）
- KPI Dashboard / ECharts / Data Export

---

## v6.0 AI（AI アシスタント）

**目的：** AI による意思決定支援を導入し、人的な承認・分析作業の負荷を軽減する。

- AI Approval Recommendation（AI 承認推奨）
- AI Financial Analysis（AI 財務分析）
- AI Cash Flow Prediction（AI キャッシュフロー予測）
- AI Investment Analysis（AI 投資分析）
- AI Risk Alert（AI リスクアラート）
- Natural Language Query（自然言語クエリ）

---

## Dashboard 主要指標

- 法人現金残高 / 月次キャッシュフロー / 資金フロー予測
- Investment Portfolio（QQQM、VOO、VTI デモデータ）
- Portfolio Performance（収益率）/ Max Drawdown（最大ドローダウン）
- Sharpe Ratio（シャープレシオ）/ Risk Analysis（リスク分析）

---

## 命名規約

- ❌ 株取引 → ✅ Investment Portfolio
- ❌ 株を買う → ✅ Asset Allocation
- ❌ 損益 → ✅ Portfolio Performance
- ❌ 私の株 → ✅ Treasury Investment

---

> 更新日：2026-07-05
