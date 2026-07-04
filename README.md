# SmartOA — シンプル OA 承認フロー管理システム

エンタープライズ級 OA 承認ワークフロー管理システム | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/Tests-24_passed-brightgreen" alt="Tests"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

---

## プロジェクト概要

SmartOA は企業の日常業務向けの**シンプルな承認フロー管理システム**で、JWT 認証、承認テンプレート管理、休暇申請と多段階承認フローをサポートします。コア設計は「テンプレート設定 + フローエンジン」を中心に展開し、条件分岐、並行承認、タイムアウト自動エスカレーションなどの高度な機能をサポートします。

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
│   │   ├── config/              # セキュリティ設定、CORS、JWT フィルター
│   │   ├── controller/          # REST コントローラー（5つ）
│   │   ├── dto/                 # データ転送オブジェクト
│   │   ├── entity/              # エンティティクラス（7つ）
│   │   ├── mapper/              # MyBatis-Plus Mapper（7つ）
│   │   └── service/             # ビジネスロジック層（5つ）+ TimeoutScheduler
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

プロジェクトには **24 件のユニットテスト**が含まれており、承認フローの主要なシナリオをカバーしています。

```bash
# 全テスト実行
cd backend && ./mvnw test

# 特定のテストクラスのみ実行
./mvnw test -Dtest=LeaveServiceTest
./mvnw test -Dtest=UserServiceTest

# 特定のテストメソッドのみ実行
./mvnw test -Dtest=LeaveServiceTest#testAdminSubmitLeave_ShouldSkipDirectLeaderNode
```

### テストカバレッジ

| テストクラス | ケース数 | カバー機能 |
|-------------|---------|-----------|
| LeaveServiceTest | 18 | 承認フロー全操作 |
| UserServiceTest | 5 | ログイン・ユーザー管理 |
| SmartoaApplicationTests | 1 | アプリケーション起動 |

**LeaveServiceTest 内訳：**
- 申請提出: 4件（adminノードスキップ、社員通常、条件分岐）
- 承認操作: 5件（承認、却下、権限超越、重複、記録保存）
- 取下げ: 3件（正常、非申請者、却下済み）
- 転送: 2件（正常、非承認者）
- 照会: 3件（詳細、記録、保留リスト）
- 滞留修復: 1件

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

### テスト

- [x] **ユニットテスト** — JUnit 5 + Spring Boot Test、24件のテストケース
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

---

## ライセンス

MIT License

---

## 開発履歴

このブランチ（`github-jp`）は純日本語の最終納品版です。開発履歴（66コミット、中日バイリンガル文書含む）をご覧になる場合は、[`github`](https://github.com/qianlixunbai/SmartOA/tree/github) ブランチをご参照ください。
