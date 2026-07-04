# SmartOA — シンプル OA 承認フロー管理システム

エンタープライズ級 OA 承認ワークフロー管理システム | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/Tests-41_passed-brightgreen" alt="Tests"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

---

## プロジェクト概要

SmartOA は企業の日常業務向けの**承認フロー管理システム**で、JWT 認証、承認テンプレート管理、休暇申請、経費精算、複式簿記をサポートします。コア設計は「テンプレート設定 + フローエンジン」を中心に展開し、条件分岐、並行承認、タイムアウト自動エスカレーションなどの高度な機能をサポートします。P3 では**金融グレードの経費精算モジュール**を追加し、複式簿記・BigDecimal 精度制御・楽観ロック・状態マシン・赤字消し戻し・監査ログを実装しています。

---

## 技術スタック

| 階層 | 技術 |
|------|------|
| バックエンドフレームワーク | Spring Boot 3.5.14 |
| 永続層 | MyBatis-Plus 3.5.15 |
| データベース | MySQL 8.0 |
| 認証・認可 | JWT（jjwt 0.13.0）+ BCrypt |
| テスト | JUnit 5 + Spring Boot Test |
| フロントエンドフレームワーク | Vue 3.5（Composition API） |
| UI コンポーネントライブラリ | Element Plus 2.13.7 |
| ビルドツール | Vite 8 |
| パッケージ管理 | pnpm |
| 状態管理 | Pinia |
| ルーティング | Vue Router 5 |

---

## プロジェクト構成

```
smartoa/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/              # Result<T>、BusinessException、GlobalExceptionHandler
│   │   ├── config/              # セキュリティ設定、CORS、JWT フィルター、楽観ロック
│   │   ├── controller/          # REST コントローラー（6つ）
│   │   ├── dto/                 # データ転送オブジェクト
│   │   ├── entity/              # エンティティクラス（11つ）
│   │   ├── mapper/              # MyBatis-Plus Mapper（11つ）
│   │   └── service/             # ビジネスロジック層（6つ）+ TimeoutScheduler
│   ├── src/test/java/com/smartoa/service/
│   │   ├── LeaveServiceTest.java   # 承認フローテスト（18ケース）
│   │   └── UserServiceTest.java    # ユーザーログインテスト（5ケース）
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                 # API ラッパー
│       ├── stores/              # Pinia 状態管理
│       ├── router/              # ルーティング設定
│       ├── views/               # ページコンポーネント（15ページ）
│       ├── components/          # 共有コンポーネント
│       ├── styles/              # グローバル CSS 変数
│       └── layouts/             # レイアウトコンポーネント
├── docs/                        # SQL マイグレーションスクリプト
├── CLAUDE.md
└── README.md
```

---

## テスト

プロジェクトには **41 件のユニットテスト**が含まれており、承認フロー・経費精算・複式簿記の主要なシナリオをカバーしています。

```bash
# 全テスト実行
cd backend && ./mvnw test

# 特定のテストクラスのみ実行
./mvnw test -Dtest=LeaveServiceTest
./mvnw test -Dtest=UserServiceTest
./mvnw test -Dtest=AccountingServiceTest
./mvnw test -Dtest=ExpenseServiceTest
```

### テストカバレッジ

| テストクラス | ケース数 | カバー機能 |
|-------------|---------|-----------|
| LeaveServiceTest | 18 | 承認フロー全操作 |
| AccountingServiceTest | 11 | 複式簿記（記帳・取消・試算平衡・残高） |
| ExpenseServiceTest | 6 | 経費精算（提出・取下げ・却下） |
| UserServiceTest | 5 | ログイン・ユーザー管理 |
| SmartoaApplicationTests | 1 | アプリケーション起動 |

**AccountingServiceTest 内訳：**
- 記帳: 5件（正常、精度、ゼロ値、負値、科目別）
- 取消: 3件（正常、重複取消、不存在）
- 試算平衡: 2件（複数記帳、記帳+取消後のバランス）
- 科目残高: 1件

**ExpenseServiceTest 内訳：**
- 提出: 3件（正常、ゼロ値、負値）
- 取下げ: 2件（正常、非申請者却下）
- 却下: 1件（承認者による却下）

---

## データベース設計

| テーブル名 | 説明 |
|------|------|
| `sys_user` | ユーザーテーブル（直属上司、部門ディレクター関連を含む） |
| `approval_template` | 承認テンプレートテーブル |
| `approval_node` | 承認ノードテーブル（条件式、承認モード、タイムアウト設定をサポート）⭐ コア |
| `template_field` | テンプレートフィールドテーブル |
| `leave_request` | 休暇申請テーブル（current_node_id + timeout_time でフローを駆動） |
| `approval_record` | 承認記録テーブル |
| `approval_task` | 並行承認タスクテーブル |
| `account` | 勘定科目テーブル（10件のシードデータ） |
| `journal_entry` | 仕訳テーブル（複式簿記コア、楽観ロック付き） |
| `expense_request` | 経費精算申請テーブル（楽観ロック付き） |
| `expense_approval_task` | 経費承認並行タスクテーブル |
| `audit_log` | 監監査ログテーブル（追加のみ、更新・削除不可） |

---

## クイックスタート

### 環境要件

- Java 21+
- MySQL 8.0+
- Node.js 18+ / pnpm

### 1. データベース作成

```sql
CREATE DATABASE smartoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

その後、`docs/` 配下の SQL スクリプトを順にインポートします。

### 2. バックエンド起動

```bash
cd backend && ./mvnw spring-boot:run
```

デフォルトポート `8080`。

### 3. フロントエンド起動

```bash
cd frontend
pnpm install
pnpm run dev
```

デフォルトポート `5173`、バックエンドへのプロキシ転送設定済み。

### 4. ログイン

ブラウザで `http://localhost:5173` を開き、以下のアカウントでログインします：

| ユーザー名 | パスワード | ロール | 説明 |
|--------|------|------|------|
| admin | 123456 | MANAGER | 技術部マネージャー（直属上司なし） |
| zhangsan | 123456 | EMPLOYEE | 一般社員（直属上司=admin） |
| lisi | 123456 | EMPLOYEE | 製品部社員 |

---

## 実装済み機能

### P0 基本機能

- [x] ユーザーログイン（JWT + BCrypt + ロール区別）
- [x] 承認テンプレート CRUD
- [x] 休暇申請の提出
- [x] ハードコードされた二段階承認フロー
- [x] 自分の未処理 / 処理済み / 提出した申請
- [x] 承認詳細ページ

### P1 アップグレード機能

- [x] 8テーブルのデータベース設計
- [x] 設定可能な多段階承認エンジン（approval_node テーブル駆動、動的ノード巡回）
- [x] 承認 / 却下 / 取下げ / 転送の4操作
- [x] 承認ノード設定 UI（テンプレート編集時にノードの追加/削除/ドラッグ＆ドロップ並べ替え）
- [x] フロー進行状況バー（`el-steps`、動的ノードステータス）
- [x] 承認履歴タイムライン（`el-timeline`、操作タイプを色分け表示）
- [x] ECharts 統計グラフ
- [x] バックエンド Excel エクスポート（Apache POI）
- [x] `Result<T>` 統一レスポンス + BusinessException グローバル例外処理

### P2 アップグレード機能

- [x] **フローノードビジュアルエディタ** — ドラッグ＆ドロップ並べ替え、動的ノード追加/削除
- [x] **条件分岐** — SpEL 式駆動（休暇日数 `days`、休暇タイプ `leaveType` などによる条件分岐をサポート）
- [x] **並行承認** — 単人 / 会簽 / 或簽 の3種類の承認モード
- [x] **タイムアウト自動エスカレーション** — ESCALATE / AUTO_APPROVE / AUTO_REJECT、`@Scheduled` で5分ごとにチェック
- [x] **滞留修復** — `repairStuckRequests()` で `currentApproverId` が null の異常滞留申請を修復

### P3 経費精算 + 複式簿記

- [x] **複式簿記エンジン** — 各経費精算が自動的に借方・貸方の仕訳を生成（SUM(debit) == SUM(credit) を保証）
- [x] **BigDecimal 精度制御** — DECIMAL(19,2)、`setScale(2, HALF_UP)`、金額に double/float を使用しない
- [x] **楽観ロック** — `@Version` アノテーション + `OptimisticLockerInnerInterceptor`（同時承認の競合を防止）
- [x] **状態マシン** — DRAFT → PENDING → APPROVED → POSTED（記帳後は変更不可、赤字消し戻しのみ）
- [x] **赤字消し戻し** — 原仕訳の借方・貸方を入れ替えた反対仕訳を生成（`memo="消込#元取引ID"`）
- [x] **監査ログ** — 追加のみ（UPDATE / DELETE 不可）、全操作を記録（SUBMIT / APPROVE / REJECT / WITHDRAW / POST / REVERSE）
- [x] **承認フロー共通化** — `approval_node` テーブルを共用し、経費用に独立した `expense_approval_task` を使用
- [x] **SpEL 条件分岐** — `ExpenseConditionVars(amount, category)` で金額・カテゴリ条件をサポート
- [x] **記帳自動化** — 承認完了時に `AccountingService.post()` を自動呼び出し、仕訳を生成

### テスト
- [x] **承認フローテスト** — 申請提出、承認、却下、取下げ、転送、照会を網羅
- [x] **異常系テスト** — 権限超越、重複操作、不正パラメータの検証
- [x] **データ整合性** — `@Transactional` によるテスト後の自動ロールバック

---

## API 概要

| メソッド | パス | 説明 |
|------|------|------|
| POST | `/api/login` | ログイン |
| GET | `/api/users` | ユーザー一覧 |
| GET | `/api/templates` | テンプレート一覧 |
| POST | `/api/templates` | テンプレート作成 |
| GET | `/api/templates/{id}/nodes` | 承認ノード取得 |
| POST | `/api/templates/{id}/nodes` | 承認ノード保存 |
| POST | `/api/leave/submit` | 休暇申請提出 |
| POST | `/api/leave/approve` | 休暇承認 |
| POST | `/api/leave/{id}/withdraw` | 取下げ |
| POST | `/api/leave/{id}/transfer` | 転送 |
| GET | `/api/leave/pending` | 未承認一覧 |
| GET | `/api/leave/done` | 承認済み一覧 |
| GET | `/api/leave/my-requests` | 自分の申請 |
| GET | `/api/leave/{id}` | 休暇申請詳細 |
| GET | `/api/leave/{id}/records` | 承認記録 |
| GET | `/api/leave/{id}/tasks` | 並行承認タスク |
| POST | `/api/leave/repair` | 滞留修復 |
| GET | `/api/stats/summary` | 統計サマリー |
| GET | `/api/stats/export` | Excel エクスポート |
| POST | `/api/expense/submit` | 経費精算申請提出 |
| POST | `/api/expense/approve` | 経費承認・却下 |
| POST | `/api/expense/{id}/withdraw` | 経費取下げ |
| POST | `/api/expense/{id}/reverse` | 赤字消し戻し（管理者のみ） |
| GET | `/api/expense/my-expenses` | 自分の経費一覧 |
| GET | `/api/expense/pending` | 未承認経費一覧 |
| GET | `/api/expense/all` | 全経費一覧（管理者） |
| GET | `/api/expense/{id}` | 経費詳細 |
| GET | `/api/expense/{id}/audit-logs` | 監査ログ |
| GET | `/api/accounting/trial-balance` | 試算平衡表 |
| GET | `/api/accounting/balances` | 科目残高一覧 |

---

## ライセンス

MIT License

---

## 開発履歴

このブランチ（`github-jp`）は純日本語の最終納品版です。開発履歴（66コミット、中日バイリンガル文書含む）をご覧になる場合は、[`github`](https://github.com/qianlixunbai/SmartOA/tree/github) ブランチをご参照ください。
