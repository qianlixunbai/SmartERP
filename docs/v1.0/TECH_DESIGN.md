# システムアーキテクチャ設計ドキュメント

## SmartOA 承認フロー管理システム

---

## 1. アーキテクチャ概要

```
┌─────────────────────────────────────────────────────────┐
│              ブラウザ (Client)                            │
│              Vue 3 + Element Plus + ECharts              │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / JSON
                       │ JWT Bearer Token
┌──────────────────────▼──────────────────────────────────┐
│                  Spring Boot 3.5.14                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ Controller│──│  Service  │──│  Mapper (MyBatis-Plus) │  │
│  │  (REST)  │  │ (Business)│  │     (Data Access)     │  │
│  └──────────┘  └──────────┘  └──────────┬───────────┘  │
│                                         │               │
│  ┌──────────┐  ┌───────────────┐        │               │
│  │ JwtFilter │  │TimeoutScheduler│       │               │
│  │  (Auth)  │  │ (@Scheduled)  │        │               │
│  └──────────┘  └───────────────┘        │               │
└─────────────────────────────────────────┼───────────────┘
                                          │ JDBC
┌─────────────────────────────────────────▼───────────────┐
│                     MySQL 8 (smartoa)                     │
│   sys_user │ approval_template │ approval_node           │
│   leave_request │ approval_record │ template_field       │
│   approval_task (並行承認)                                  │
└─────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────┐
│                      ブラウザ (Client)                      │
│              Vue 3 + Element Plus + ECharts              │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / JSON
                       │ JWT Bearer Token
┌──────────────────────▼──────────────────────────────────┐
│                  Spring Boot 3.5.14                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ Controller│──│  Service  │──│  Mapper (MyBatis-Plus) │  │
│  │  (REST)  │  │ (Business)│  │     (Data Access)     │  │
│  └──────────┘  └──────────┘  └──────────┬───────────┘  │
│                                         │               │
│  ┌──────────┐  ┌───────────────┐        │               │
│  │ JwtFilter │  │TimeoutScheduler│       │               │
│  │  (Auth)  │  │ (@Scheduled)  │        │               │
│  └──────────┘  └───────────────┘        │               │
└─────────────────────────────────────────┼───────────────┘
                                          │ JDBC
┌─────────────────────────────────────────▼───────────────┐
│                     MySQL 8 (smartoa)                     │
│   sys_user │ approval_template │ approval_node           │
│   leave_request │ approval_record │ template_field       │
│   approval_task (並行承認)                                │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 技術選定

## 2. 技術選定

|---|---|---|---|
| 認証 | JJWT | 0.13.0 | 軽量 JWT 実装 |

| レイヤー | 技術 | バージョン | 選定理論 |
|---|---|---|---|
| バックエンドフレームワーク | Spring Boot | 3.5.14 | エンタープライズ Java 標準フレームワーク |
| ORM | MyBatis-Plus | 3.5.15 | 柔軟なクエリ + Lambda タイプセーフ + 自動 CRUD |
| データベース | MySQL | 8.x | 成熟して安定、utf8mb4 対応 |
| 認証 | JJWT | 0.13.0 | 軽量 JWT 実装 |
| パスワード暗号化 | BCrypt | spring-security-crypto | 単方向ハッシュ、不可逆 |
| フロントエンドフレームワーク | Vue 3 | 3.5 | Composition API + リアクティブ |
| ビルドツール | Vite | 8.x | 高速 HMR、ESM ネイティブ |
| UI ライブラリ | Element Plus | 2.13.7 | エンタープライズ Vue 3 コンポーネントライブラリ |
| 状態管理 | Pinia | 3.x | Vue 3 公式推奨 |
| チャート | ECharts | 5.x | 機能充実、中国語対応 |
| Excel | Apache POI | 5.3 | Java Excel 読み書き標準ライブラリ |
| パッケージ管理 | pnpm | — | 高速、ディスク容量節約 |

---

## 3. パッケージ構造

### 3.1 バックエンド（backend/src/main/java/com/smartoa/）

```
com.smartoa
├── SmartoaApplication.java
├── common/
│   ├── Result.java
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── config/
│   ├── JwtProperties.java        # JWT 秘密鍵+有効期限設定
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   ├── UserContextHolder.java
│   └── WebConfig.java            # CORS 設定 + Filter 登録
├── controller/
│   ├── UserController.java
│   ├── LeaveController.java
│   ├── TemplateController.java
│   ├── StatsController.java
│   └── ExportController.java
├── service/
│   ├── UserService.java
│   ├── LeaveService.java
│   ├── TemplateService.java
│   ├── StatsService.java         # 統計計算
│   ├── ExportService.java        # Excel 生成
│   └── TimeoutScheduler.java
├── entity/
│   ├── User.java
│   ├── ApprovalTemplate.java
│   ├── ApprovalNode.java
│   ├── TemplateField.java
│   ├── LeaveRequest.java
│   ├── ApprovalRecord.java
│   └── ApprovalTask.java
├── mapper/
│   ├── UserMapper.java
│   ├── LeaveRequestMapper.java
│   ├── ApprovalRecordMapper.java
│   ├── ApprovalTemplateMapper.java
│   ├── ApprovalNodeMapper.java
│   ├── TemplateFieldMapper.java
│   └── ApprovalTaskMapper.java
└── dto/
    ├── LoginDTO.java
    └── LeaveSubmitDTO.java
```

```
com.smartoa
├── SmartoaApplication.java       # 起動クラス + @MapperScan + @EnableScheduling
├── common/
│   ├── Result.java                # 統一レスポンス {code, message, data}
│   ├── BusinessException.java     # ビジネス例外
│   └── GlobalExceptionHandler.java # @RestControllerAdvice グローバル例外処理
├── config/
│   ├── JwtProperties.java        # JWT 秘密鍵+有効期限設定
│   ├── JwtUtil.java              # Token 生成/検証/解析ツール
│   ├── JwtFilter.java            # リクエスト認証フィルター
│   ├── UserContextHolder.java    # ThreadLocal で現在のユーザーIDを保存
│   └── WebConfig.java            # CORS 設定 + Filter 登録
├── controller/
│   ├── UserController.java       # ログイン/ログアウト/ユーザー一覧
│   ├── LeaveController.java      # 休暇申請/承認/取り消し/転送/滞留修復/並行タスク
│   ├── TemplateController.java   # テンプレートCRUD/ノード/フィールド
│   ├── StatsController.java      # 統計データ
│   └── ExportController.java     # Excel エクスポート
├── service/
│   ├── UserService.java          # ユーザービジネスロジック
│   ├── LeaveService.java         # 承認エンジンコア（並行+タイムアウト+条件分岐含む）
│   ├── TemplateService.java      # テンプレート+ノード+フィールド管理
│   ├── StatsService.java         # 統計計算
│   ├── ExportService.java        # Excel 生成
│   └── TimeoutScheduler.java     # タイムアウト定期チェック（@Scheduled 5分）
├── entity/
│   ├── User.java                 # ユーザーエンティティ (sys_user)
│   ├── ApprovalTemplate.java     # テンプレートエンティティ (approval_template)
│   ├── ApprovalNode.java         # ノードエンティティ (approval_node) — 承認モード+条件+タイムアウト含む
│   ├── TemplateField.java        # フィールドエンティティ (template_field)
│   ├── LeaveRequest.java         # 申請エンティティ (leave_request) — timeoutTime 含む
│   ├── ApprovalRecord.java       # 記録エンティティ (approval_record)
│   └── ApprovalTask.java         # 並行承認タスクエンティティ (approval_task)
├── mapper/
│   ├── UserMapper.java
│   ├── LeaveRequestMapper.java
│   ├── ApprovalRecordMapper.java
│   ├── ApprovalTemplateMapper.java
│   ├── ApprovalNodeMapper.java
│   ├── TemplateFieldMapper.java
│   └── ApprovalTaskMapper.java
└── dto/
    ├── LoginDTO.java              # ログインリクエストボディ
    └── LeaveSubmitDTO.java        # 休暇申請リクエストボディ
```

### 3.2 フロントエンド（frontend/src/）

```
src
├── App.vue
├── main.js
├── api/
│   ├── index.js
│   ├── auth.js
│   ├── leave.js
│   ├── template.js
│   └── stats.js
├── stores/
│   ├── auth.js                    # 認証状態（token/user/role）
│   ├── approval.js
│   └── users.js
├── router/
│   └── index.js
├── layouts/
│   └── MainLayout.vue
├── components/
│   ├── AppHeader.vue
│   ├── AppSidebar.vue
│   ├── ApprovalTimeline.vue
│   └── StatusTag.vue
├── views/
│   ├── LoginPage.vue
│   ├── SubmitApplicationPage.vue
│   ├── MyApprovalsPage.vue
│   ├── ApprovalDetailPage.vue
│   ├── TemplateListPage.vue
│   ├── TemplateEditPage.vue
│   └── StatsPage.vue
└── utils/
    └── constants.js
```

```
src
├── App.vue                        # ルートコンポーネント + グローバル provide
├── main.js                        # エントリポイント：Pinia + Router + Element Plus
├── api/
│   ├── index.js                   # axios インスタンス + インターセプター
│   ├── auth.js                    # 認証インターフェース
│   ├── leave.js                   # 休暇インターフェース（tasks/repair 含む）
│   ├── template.js                # テンプレートインターフェース
│   └── stats.js                   # 統計インターフェース
├── stores/
│   ├── auth.js                    # 認証状態（token/user/role）
│   ├── approval.js                # 承認データ状態（currentTasks 含む）
│   └── users.js                   # ユーザー一覧 + ID→名前マッピング
├── router/
│   └── index.js                   # ルート設定 + ナビゲーションガード
├── layouts/
│   └── MainLayout.vue             # メインレイアウト（Header + Sidebar + Content）
├── components/
│   ├── AppHeader.vue              # ヘッダーバー（ユーザー情報/ログアウト）
│   ├── AppSidebar.vue             # サイドバーナビゲーション
│   ├── ApprovalTimeline.vue       # 承認タイムライン
│   └── StatusTag.vue              # ステータスタグ
├── views/
│   ├── LoginPage.vue              # ログインページ
│   ├── SubmitApplicationPage.vue  # 申請提出ページ
│   ├── MyApprovalsPage.vue        # 自分の承認ページ
│   ├── ApprovalDetailPage.vue     # 承認詳細ページ（並行承認者タグ+タイムアウト警告含む）
│   ├── TemplateListPage.vue       # テンプレート一覧ページ
│   ├── TemplateEditPage.vue       # テンプレート編集ページ（ビジュアルフローエディタ含む）
│   └── StatsPage.vue              # 統計レポートページ
└── utils/
    └── constants.js               # フロントエンド定数（状態/アクション/承認モード/タイムアウトアクションマッピング）
```

---

## 4. コア設計

### 4.1 承認エンジンフロー

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
└──────────┘     └──────────────┘     └────────┬─────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 承認者解決 (resolveApprovers) │
                                │ ├─ SINGLE → 単独承認者         │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ SINGLE: currentApproverId 設定 │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 承認者操作                     │
                                │ ├─ WITHDRAW → 終了 (申請者)    │
                                │ └─ TRANSFER → 承認者置換(SINGLE) │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                └─────────────────────────────┘
```

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
│ 申請提出  │────▶│ テンプレートノード読込 │────▶│ 条件分岐評価(SpEL) │
└──────────┘     └──────────────┘     └────────┬─────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 承認者解決 (resolveApprovers) │
                                │ ├─ SINGLE → 単独             │
                                │ ├─ COUNTER_SIGN → 複数人リスト    │
                                │ └─ OR_SIGN → 複数人リスト        │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ SINGLE: currentApproverId 設定  │
                                │ 並行: approval_task 一括挿入   │
                                │ timeoutTime 設定（設定がある場合）   │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 承認者操作                     │
                                │ ├─ APPROVE → 進行/並行チェック     │
                                │ ├─ REJECT → 終了+並行タスクスキップ  │
                                │ ├─ WITHDRAW → 終了 (申請者)   │
                                │ └─ TRANSFER → 承認者置換(SINGLE)│
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 次のノードあり？                  │
                                │ ├─ はい → 遷移、承認待ち継続    │
                                │ └─ いいえ → APPROVED、フロー終了   │
                                └─────────────────────────────┘
```

  - `DIRECT_LEADER` → `applicant.directLeaderId`
  - `DEPARTMENT_HEAD` → `applicant.departmentHeadId`

**キーメソッド：**
- `advanceToNextNode()` — 次の条件を満たすノードを検索、承認者を解決、SINGLE/並行モードを処理
- `resolveApprovers()` — `approverType` + `signType` に基づいて承認者リストを解決
  - `DIRECT_LEADER` → `applicant.directLeaderId`
  - `DEPARTMENT_HEAD` → `applicant.departmentHeadId`
  - `SPECIFIC_USER` → SINGLE: `node.approverId`；並行: `node.approverIds` カンマ区切り
- `evaluateCondition()` — SpEL 式評価、`leaveType`、`days`、`startDate`、`endDate` 変数をサポート
- `checkTimeouts()` — `timeoutTime` が期限切れの PENDING 申請をスキャン
- `processTimeout()` — タイムアウトアクションを実行（ESCALATE/AUTO_APPROVE/AUTO_REJECT）

### 4.2 認証フロー

```
  │
  ▼
  ├─ /api/login → 通過
  └─ 検証成功 → UserContextHolder.setUserId() → Controller
```

```
ユーザーログイン → POST /api/login → BCrypt パスワード検証 → JWT 生成
  │
  ▼
後続リクエスト → JwtFilter インターセプト
  ├─ /api/login → 通過
  ├─ Authorization ヘッダーなし → 401
  ├─ Token 無効/期限切れ → 401
  ├─ ユーザー不在 → 401
  └─ 検証成功 → UserContextHolder.setUserId() → Controller
```

### 4.3 並行承認モード

|---|---|---|

| モード | 進行条件 | 説明 |
|---|---|---|
| SINGLE | 単独承認 | 既存の動作を維持、`currentApproverId` で駆動 |
| COUNTER_SIGN | 全員同意 | 各承認者に `approval_task` を作成、全員 COMPLETED 後に進行 |
| OR_SIGN | いずれか同意 | いずれかが APPROVE → 進行、残りのタスクは SKIPPED |

### 4.4 タイムアウト自動エスカレーション

```
TimeoutScheduler (@Scheduled 5min)
  → LeaveService.checkTimeouts()
    → 検索 status=PENDING AND timeout_time < NOW()
      → processTimeout()
        ├─ AUTO_APPROVE → 自動承認
        └─ AUTO_REJECT → 自動却下
```

```
TimeoutScheduler (@Scheduled 5min)
  → LeaveService.checkTimeouts()
    → status=PENDING AND timeout_time < NOW() を検索
      → processTimeout()
        ├─ ESCALATE → 転送またはノードをスキップ
        ├─ AUTO_APPROVE → 自動承認
        └─ AUTO_REJECT → 自動却下
```

### 4.5 フロントエンド名前マッピング

```
       │
       ▼
App.vue provide('getUserName', id => userStore.getUserName(id))
       │
       ▼
```

```
バックエンドの返却データは ID のみ（applicantId, approverId）
       │
       ▼
App.vue provide('getUserName', id => userStore.getUserName(id))
       │
       ▼
各コンポーネント inject('getUserName') → ID を名前に変換して表示
```

---

## 5. セキュリティ設計

| 対策 | 説明 |
|---|---|

| 対策 | 説明 |
|---|---|
| JWT 認証 | すべての /api/* エンドポイント（/api/login 除く）に Bearer Token 必須 |
| パスワード暗号化 | BCrypt 単方向ハッシュ、不可逆 |
| パスワード保護 | @JsonIgnore でパスワードのシリアライズ返却を防止 |
| CORS | http://localhost:5173 のみクロスオリジン許可 |
| 権限チェック | テンプレート管理/エクスポートエンドポイントで role=MANAGER をチェック |
| 操作チェック | 取り消しは申請者をチェック、承認は現在の承認者/並行タスクをチェック |
| 統一レスポンス | `Result<T>` {code, message, data}、グローバル例外処理 |

---

## 6. デプロイアーキテクチャ

```
┌────────────────────────────────────┐
│  ┌────────────┐  ┌────────────┐    │
│  │ Vite Dev   │  │ Spring Boot│    │
│  │ :5173      │──│ :8080      │    │
│  └────────────┘  └─────┬──────┘    │
│                        │            │
│                 ┌──────▼──────┐     │
│                 │  MySQL :3306│     │
│                 └─────────────┘     │
└────────────────────────────────────┘
```

```
┌────────────────────────────────────┐
│  開発環境 (スタンドアロン)                     │
│  ┌────────────┐  ┌────────────┐    │
│  │ Vite Dev   │  │ Spring Boot│    │
│  │ :5173      │──│ :8080      │    │
│  └────────────┘  └─────┬──────┘    │
│                        │            │
│                 ┌──────▼──────┐     │
│                 │  MySQL :3306│     │
│                 └─────────────┘     │
└────────────────────────────────────┘
```

|---|---|---|

| コンポーネント | ポート | 起動コマンド |
|---|---|---|
| フロントエンド | 5173 | `pnpm run dev` |
| バックエンド | 8080 | `cd backend && ./mvnw spring-boot:run` |
| データベース | 3306 | MySQL サービス |

---

ドキュメントバージョン：v2.0（P2 完了） | 更新日：2026-05-27