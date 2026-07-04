# SmartOA 簡易承認フロー管理システム — 完全技術ドキュメント

**バージョン：P2 完了版** | 日付：2026-05-27 | 技術スタック：Spring Boot 3.5.14 + Vue 3 + MySQL 8.0 + JWT + MyBatis-Plus

---

## 一、データベース設計

システムは全 8 テーブル、MySQL 8.0、文字セット utf8mb4、ストレージエンジン InnoDB。

### 1.1 ユーザーテーブル（sys_user）

ユーザー情報を格納し、組織階層関係（直属上司 + 部門長）を含む。

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| username | VARCHAR(50) | ユーザー名、一意 |
| password | VARCHAR(100) | BCrypt 暗号化 |
| real_name | VARCHAR(50) | 氏名 |
| role | VARCHAR(20) | 役割：MANAGER / EMPLOYEE |
| department | VARCHAR(50) | 部門 |
| direct_leader_id | BIGINT | 直属上司 ID（自己参照） |
| department_head_id | BIGINT | 部門長 ID（自己参照） |

**シードデータ**（全ユーザーのパスワード：123456、BCrypt 暗号化）：

| ID | ユーザー名 | 氏名 | 役割 | 部門 | 直属上司 | 部門長 |
|----|--------|------|------|------|----------|----------|
| 1 | admin | 王経理 | MANAGER | 技術部 | なし | 4(張総監) |
| 2 | zhangsan | 張三 | EMPLOYEE | 技術部 | 1(王経理) | 4(張総監) |
| 3 | lisi | 李四 | EMPLOYEE | 製品部 | 1(王経理) | 5(李総監) |
| 4 | zongjian1 | 張総監 | MANAGER | 技術部 | なし | なし |
| 5 | zongjian2 | 李総監 | MANAGER | 製品部 | なし | なし |

### 1.2 承認テンプレートテーブル（approval_template）

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| name | VARCHAR(100) | テンプレート名 |
| description | VARCHAR(500) | テンプレート説明 |
| enabled | BIT | 有効かどうか |
| create_time | DATETIME | 作成日時 |
| update_time | DATETIME | 更新日時 |

### 1.3 承認ノードテーブル（approval_node）⭐ コア

テンプレート配下の承認ステップ。`approval_template` から外部キー `template_id` で 1:N 関連。条件分岐、並列承認、タイムアウト設定をサポート。

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| template_id | BIGINT FK | 所属テンプレート ID |
| node_name | VARCHAR(100) | ノード名 |
| sort_order | INT | ソート順序 |
| approver_type | VARCHAR(20) | DIRECT_LEADER / DEPARTMENT_HEAD / SPECIFIC_USER |
| approver_id | BIGINT | SPECIFIC_USER 時に指定するユーザー ID |
| condition_expression | VARCHAR(500) | SpEL 条件式（null 可、例：`days > 3`） |
| sign_type | VARCHAR(20) | SINGLE（単人）/ COUNTER_SIGN（会簽）/ OR_SIGN（或簽） |
| approver_ids | VARCHAR(1000) | 並列署名時の承認者 ID リスト、カンマ区切り |
| timeout_hours | INT | タイムアウト時間数、NULL=無効 |
| timeout_action | VARCHAR(20) | ESCALATE / AUTO_APPROVE / AUTO_REJECT |
| escalate_to_user_id | BIGINT | タイムアウト転送先ユーザー ID |
| create_time | DATETIME | 作成日時 |
| update_time | DATETIME | 更新日時 |

**条件式変数**：`leaveType`（休暇種類 String）、`days`（休暇日数 long）、`startDate`、`endDate`（LocalDate）

例：`days > 3` → 3日を超える休暇はこのノードを通る；`leaveType == '病欠'` → 病欠はこのノードを通る。

### 1.4 休暇申請テーブル（leave_request）

コアテーブル。`current_node_id` + `current_approver_id` + `timeout_time` が承認フロー全体を駆動する。

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| applicant_id | BIGINT FK | 申請者 ID → sys_user.id |
| template_id | BIGINT FK | 使用テンプレート ID |
| leave_type | VARCHAR(20) | 休暇種類 |
| start_date | DATE | 開始日 |
| end_date | DATE | 終了日 |
| reason | VARCHAR(500) | 休暇理由 |
| status | VARCHAR(20) | PENDING / APPROVED / REJECTED / WITHDRAWN |
| approval_step | INT | 現在の承認ステップ番号 |
| current_node_id | BIGINT | 現在の承認ノード ID（フロー駆動） |
| current_approver_id | BIGINT | 現在の承認者 ID（SINGLE モード、並列モードでは null） |
| timeout_time | DATETIME | 現在のノードのタイムアウト期限（null 可） |
| create_time | DATETIME | 作成日時 |
| update_time | DATETIME | 更新日時 |

### 1.5 承認記録テーブル（approval_record）

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| leave_request_id | BIGINT FK | 休暇申請 ID |
| approver_id | BIGINT FK | 承認者 ID（0=システム自動） |
| action | VARCHAR(20) | APPROVE / REJECT / WITHDRAW / TRANSFER / TIMEOUT_* |
| comment | VARCHAR(500) | 承認コメント |
| approval_step | INT | 承認ステップ番号 |
| node_id | BIGINT | 承認ノード ID |
| create_time | DATETIME | 承認日時 |

### 1.6 テンプレートフィールドテーブル（template_field）

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| template_id | BIGINT FK | 所属テンプレート ID |
| field_name | VARCHAR(50) | フィールド名 |
| field_label | VARCHAR(50) | フィールドラベル |
| field_type | VARCHAR(20) | TEXT / NUMBER / DATE / SELECT |
| sort_order | INT | ソート順序 |
| required | BIT | 必須かどうか |
| options | VARCHAR(500) | オプション（JSON、SELECT タイプ用） |

### 1.7 並列承認タスクテーブル（approval_task）⭐ P2 新規

並列承認（会簽/或簽）モード時、各承認者の承認状態を追跡する。

| フィールド | 型 | 説明 |
|------|------|------|
| id | BIGINT PK | 自動採番主キー |
| leave_request_id | BIGINT FK | 休暇申請 ID |
| node_id | BIGINT FK | 承認ノード ID |
| approver_id | BIGINT FK | 承認者 ID |
| status | VARCHAR(20) | PENDING / COMPLETED / SKIPPED |
| create_time | DATETIME | 作成日時 |
| update_time | DATETIME | 更新日時 |

制約：`UNIQUE (leave_request_id, node_id, approver_id)`

---

## 二、バックエンドコアロジック

### 2.1 JWT + BCrypt 認証フロー

関連ファイル：`JwtProperties.java`、`JwtUtil.java`、`UserContextHolder.java`、`JwtFilter.java`、`WebConfig.java`

**ログインフロー：**

1. ユーザーが `POST /api/login` でユーザー名・パスワードを送信
2. `UserService.login()` が `sys_user` テーブルを検索し、`BCryptPasswordEncoder.matches()` でパスワードを照合
3. `JwtUtil.generateToken()` が JWT を生成（sub=ユーザーID、claims に username+role を含む、24h 有効期限）
4. `Result<Map>` を返却：`{code:200, data:{token, user:{id, username, realName, role, department, directLeaderId, departmentHeadId}}}`

**リクエスト認証フロー：**

1. 各 `/api/*` リクエストが `JwtFilter.doFilterInternal()` を通過
2. `Authorization: Bearer xxx` ヘッダーから token を抽出
3. jjwt ライブラリで署名 + 有効期限を検証
4. userId を解析 → `UserContextHolder`（ThreadLocal）に保存
5. Controller が `UserService.getLoginUser()` で現在のユーザーを取得
6. リクエスト終了後に `finally` ブロックで ThreadLocal をクリアし、メモリリークを防止

### 2.2 承認フローエンジン（LeaveService.java）⭐ コア

#### 休暇申請提出（submitLeave）

1. 申請者エンティティを取得（directLeaderId、departmentHeadId を含む）
2. LeaveRequest を作成：status=PENDING, approvalStep=0
3. `advanceToNextNode()` を呼び出し、最初の条件を満たす承認ノードへ進める
4. データベースに保存し、フロントエンドに返却

#### 承認操作（approveLeave）

```
検証フェーズ：
1. 休暇申請は存在するか？
2. ステータスは PENDING か？
3. 承認者検証：
   - SINGLE モード：承認者 == currentApproverId
   - 並列モード：承認者が approval_task テーブルに PENDING タスクを持つ
   - 両方満たさない → 拒否
承認ログを記録 → ApprovalRecord
フロー判定：
├── REJECT（却下）
│   ├── ステータス → REJECTED、currentApproverId/currentNodeId/timeoutTime をクリア
│   └── 並列ノード：残りの PENDING タスクをスキップ
│
├── APPROVE + OR_SIGN（或簽）
│   ├── 現在の承認者タスクを完了
│   ├── ノードの残り PENDING タスクをスキップ
│   └── 次のノードへ進む
│
├── APPROVE + COUNTER_SIGN（会簽）
│   ├── 現在の承認者タスクを完了
│   ├── 全員完了したか確認
│   ├── はい → 次のノードへ進む
│   └── いいえ → 他者の承認を待機
│
└── APPROVE + SINGLE（単人）
    └── 次のノードへ進む
次のノードへ進む（advanceToNextNode）：
1. テンプレートの全ノードを読み込み（sortOrder でソート）
2. currentNodeId の後から走査開始
3. 各ノードに対して：
   a. SpEL 条件判定を実行 → 満たさなければスキップ
   b. 承認者を解決：
      - SINGLE → approverType に従って解決（DIRECT_LEADER/DEPARTMENT_HEAD/SPECIFIC_USER）
      - 並列モード → approverIds のカンマ区切りリストを解析
   c. 承認者を割り当て：
      - SINGLE → currentApproverId を設定
      - 並列モード → currentApproverId=null、approval_task を一括挿入
   d. タイムアウト期限を設定：
      - timeout_hours があれば → timeout_time = now + timeout_hours
      - なければ → timeout_time = null
   e. true を返す
4. ノードがなくなった → false を返す（フロー完了）
```

#### その他の操作

| 操作 | メソッド | 説明 |
|------|------|------|
| 取り下げ | `withdrawLeave()` | 自分の PENDING 申請のみ取り下げ可能、現在のノードの PENDING タスクをスキップ |
| 転送 | `transferLeave()` | 単人ノードを他ユーザーに転送（並列ノードは転送不可） |
| 滞留修復 | `repairStuckRequests()` | currentApproverId が null かつ PENDING タスクがない滞留申請を修復 |
| タイムアウトチェック | `checkTimeouts()` | timeout_time が期限切れの申請を検索し、タイムアウトアクションを実行 |

#### タイムアウトアクション

| アクション | 説明 |
|------|------|
| ESCALATE | 指定者に転送（escalate_to_user_id あり）またはスキップして次のノードへ |
| AUTO_APPROVE | 現在のノードの全承認待ちタスクをスキップし、自動承認で次のノードへ |
| AUTO_REJECT | 申請を却下し、フローを終了 |

### 2.3 定期タスク（TimeoutScheduler.java）

```java
@Scheduled(fixedRate = 300000) // 5 分ごと
public void checkTimeouts() {
    int count = leaveService.checkTimeouts();
    if (count > 0) log.info("processed {} timed-out approvals", count);
}
```

### 2.4 承認テンプレート管理（TemplateService.java）

- `listAll()` → 全テンプレートを検索
- `getById(id)` → 単一テンプレートを検索、存在しなければ BusinessException をスロー
- `create(template)` → テンプレートを作成
- `update(id, data)` → name/description/enabled を更新
- `delete(id)` → テンプレート、ノード、フィールドをカスケード削除
- `listNodes(templateId)` → テンプレートの承認ノードを検索（sortOrder でソート）
- `saveNodes(templateId, nodes)` → ノードを全量置換（旧ノードを削除し参照をクリア、新ノードを挿入）
- `listFields(templateId)` → テンプレートのカスタムフィールドを検索

### 2.5 権限制御

| モジュール | 権限方式 | 説明 |
|------|----------|------|
| テンプレート管理 | Controller 層 `role == "MANAGER"` | MANAGER 以外は 403 を返す |
| 承認操作 | Service 層 `currentApproverId` / `approval_task` | 承認者に指定された人は誰でも承認可能 |
| フロントエンドルーティング | `router.beforeEach` 役割ガード | MANAGER 以外が /templates にアクセスするとホームにリダイレクト |
| CORS | WebConfig で localhost:5173 を許可 | 開発環境のクロスオリジン対応 |

### 2.6 統一レスポンス形式

```json
// 成功
{ "code": 200, "message": "操作成功", "data": { ... } }

// 業務例外
{ "code": 500, "message": "エラー理由", "data": null }

// 未ログイン
{ "code": 401, "message": "ログインしてください", "data": null }

// 権限なし
{ "code": 403, "message": "権限がありません", "data": null }
```

`BusinessException` + `@RestControllerAdvice`（GlobalExceptionHandler）によるグローバル統一例外処理。

### 2.7 インターフェース一覧（全 20+ 件）

| 番号 | パス | メソッド | 認証 | 説明 |
|------|------|------|------|------|
| 1 | `/api/login` | POST | 不要 | ログイン |
| 2 | `/api/users` | GET | 必要 | ユーザー一覧 |
| 3 | `/api/users/current` | GET | 必要 | 現在のユーザー取得 |
| 4 | `/api/leave/submit` | POST | 必要 | 休暇申請提出 |
| 5 | `/api/leave/approve` | POST | 必要 | 休暇承認 |
| 6 | `/api/leave/{id}/withdraw` | POST | 必要 | 取り下げ |
| 7 | `/api/leave/{id}/transfer` | POST | 必要 | 転送 |
| 8 | `/api/leave/repair` | POST | 必要 | 滞留申請の修復 |
| 9 | `/api/leave/all` | GET | 必要 | 全申請（MANAGER） |
| 10 | `/api/leave/my-requests` | GET | 必要 | 自分の申請 |
| 11 | `/api/leave/pending` | GET | 必要 | 承認待ち一覧 |
| 12 | `/api/leave/done` | GET | 必要 | 処理済み一覧 |
| 13 | `/api/leave/{id}` | GET | 必要 | 休暇申請詳細 |
| 14 | `/api/leave/{id}/records` | GET | 必要 | 承認記録 |
| 15 | `/api/leave/{id}/tasks` | GET | 必要 | 並列承認タスク |
| 16 | `/api/templates` | GET | 必要 | テンプレート一覧 |
| 17 | `/api/templates/{id}` | GET | 必要 | テンプレート詳細 |
| 18 | `/api/templates` | POST | MANAGER | テンプレート作成 |
| 19 | `/api/templates/{id}` | PUT | MANAGER | テンプレート更新 |
| 20 | `/api/templates/{id}` | DELETE | MANAGER | テンプレート削除 |
| 21 | `/api/templates/{id}/nodes` | GET | 必要 | 承認ノード取得 |
| 22 | `/api/templates/{id}/nodes` | POST | MANAGER | 承認ノード保存 |
| 23 | `/api/templates/{id}/fields` | GET | 必要 | テンプレートフィールド取得 |
| 24 | `/api/stats/summary` | GET | 必要 | 統計サマリー |
| 25 | `/api/stats/export` | GET | 必要 | Excel エクスポート |

---

## 三、フロントエンドアーキテクチャ

### 3.1 技術スタック

Vue 3.5（Composition API）+ Element Plus 2.13.7 + Vite 8 + Pinia + Vue Router 5 + Axios + ECharts 5

エントリーファイル `main.js`：Vue アプリケーションを作成 → Element Plus Icons を登録 → Pinia/Router/ElementPlus をインストール → `#app` にマウント

### 3.2 レイアウトシステム

| レイアウト | ファイル | 使用シーン | 構造 |
|------|------|----------|------|
| AuthLayout | `layouts/AuthLayout.vue` | /login | 全画面中央、紫グラデーション背景 |
| MainLayout | `layouts/MainLayout.vue` | ログイン以外の全ページ | 左 220px サイドバー + 上 60px ナビ + 中央グレー背景のコンテンツエリア |

### 3.3 ルーティング設定

| パス | ページ | 認証 | 役割 |
|------|------|------|------|
| `/login` | LoginPage.vue | 未認証可 | — |
| `/submit-application` | SubmitApplicationPage.vue | 必須 | — |
| `/my-approvals` | MyApprovalsPage.vue | 必須 | — |
| `/approval/:id` | ApprovalDetailPage.vue | 必須 | — |
| `/templates` | TemplateListPage.vue | 必須 | MANAGER |
| `/templates/edit/:id?` | TemplateEditPage.vue | 必須 | MANAGER |
| `/stats` | StatsPage.vue | 必須 | — |
| `/` | リダイレクト | → /submit-application | — |
| `/:pathMatch(.*)*` | NotFoundPage.vue | — | 404 |

**ルートガード（beforeEach）：**

1. token あり but ユーザーなし → 自動で `fetchUser()` を呼び出しログイン状態を復元
2. 認証必須だが未ログイン → `/login` にリダイレクト
3. ログイン済みで `/login` にアクセス → `/submit-application` にリダイレクト
4. 役割不一致 → `/submit-application` にリダイレクト

### 3.4 Axios ラッパー（api/index.js）

- baseURL = `/api`（Vite 開発サーバーが localhost:8080 にプロキシ）
- リクエストインターセプター：自動で `Authorization: Bearer <token>` を付加
- レスポンスインターセプター：`code === 200` → `data` を直接返却；`code === 401` → token をクリア + ログイン画面にリダイレクト；その他 → `ElMessage.error`

### 3.5 状態管理（Pinia）

**auth store：**

| 状態/メソッド | 説明 |
|-----------|------|
| token | JWT トークン、localStorage に永続化 |
| user | ユーザー情報オブジェクト、localStorage に永続化 |
| isManager | 算出プロパティ：`user.role === "MANAGER"` |
| login() | ログイン API を呼び出し → token+user を保存 → ホームにリダイレクト |
| fetchUser() | GET /api/users/current で現在のユーザーをリフレッシュ |
| logout() | 全状態をクリア → ログイン画面にリダイレクト |

**approval store：**

| 状態/メソッド | 説明 |
|-----------|------|
| pendingRequests / doneRequests / myRequests | 3種類の承認リスト |
| currentDetail / currentRecords / currentTasks | 現在の休暇申請詳細/記録/並列タスク |
| fetchPendingRequests() | GET /api/leave/pending |
| fetchDoneRequests() | GET /api/leave/done |
| fetchDetail(id) | GET /api/leave/{id} |
| fetchRecords(id) | GET /api/leave/{id}/records |
| fetchTasks(id) | GET /api/leave/{id}/tasks |

### 3.6 ページコンポーネント詳細

**① ログインページ（LoginPage.vue）**
- 紫グラデーション全画面背景、白い中央寄せ 400px カード
- ユーザー名 + パスワード入力欄（アイコン付き）
- Enter キーでクイックログイン対応
- 下部にテストアカウントのヒントを表示

**② 申請提出（SubmitApplicationPage.vue）**
- 2カラムレイアウト：左側フォーム + 右側自分の休暇記録
- 休暇種類ドロップダウン（6種類）+ 日付ピッカー + 理由テキストエリア
- フロントエンドバリデーション：終了日 >= 開始日、必須項目が空でないこと
- 提出成功：トースト表示 + フォームクリア + リスト更新

**③ 自分の承認（MyApprovalsPage.vue）**
- 3タブ構成：承認待ち / 処理済み / 自分の申請
- テーブル列：ID、申請者、休暇種類、日付、状態、操作

**④ 承認詳細（ApprovalDetailPage.vue）⭐ コアページ**
- 上部 `el-steps` ステップバー（動的ノード + 完了ステップ）
- 並列承認：現在のノードの全承認待ち者タグを表示
- タイムアウト通知：タイムアウト期限を表示（黄色警告バー）
- 詳細エリア `el-descriptions`（2列、枠線付き）
- 承認操作エリア（複数人対応）：
  - SINGLE モード：`currentApproverId` 一致で表示
  - 並列モード：`approval_task` に PENDING タスクがある場合に表示
- 転送ボタン + ダイアログ（SINGLE モードのみ使用可能）
- 取り下げボタン（申請者のみ表示）
- 承認記録タイムライン

**⑤ テンプレート一覧（TemplateListPage.vue）**
- テーブルでテンプレート一覧を表示
- 新規作成/編集/削除操作（MANAGER のみ）

**⑥ テンプレート編集（TemplateEditPage.vue）⭐ P2 強化**
- ルートパラメータ `:id?` は省略可能
- 基本情報フォーム：テンプレート名 + 説明 + 有効スイッチ
- **ビジュアルフローエディタ**：
  - ドラッグハンドルでノードをソート
  - 各ノードカードに含まれるもの：ノード名 + 署名モード（単人/会簽/或簽）+ 承認者タイプ/複数人セレクター
  - 条件式（SpEL）：折り畳み展開可能、例：`days > 3`
  - タイムアウト設定：折り畳み展開可能、タイムアウト時間数 + タイムアウトアクション（転送/自動承認/自動却下）+ 対象ユーザー
  - 削除ボタン（ホバー時表示）
  - ノード間接続線

**⑦ 統計ページ（StatsPage.vue）**
- ECharts グラフ：テンプレート別平均承認時間 + 各テンプレート使用量

### 3.7 共有コンポーネント

| コンポーネント | ファイル | 機能 |
|------|------|------|
| LeaveTable | `LeaveTable.vue` | 汎用承認テーブル |
| LeaveForm | `LeaveForm.vue` | 休暇フォームカプセル化 |
| ApprovalTimeline | `ApprovalTimeline.vue` | 承認タイムライン |
| StatusTag | `StatusTag.vue` | 状態タグ（PENDING=オレンジ/APPROVED=緑/REJECTED=赤/WITHDRAWN=グレー） |

---

## 四、コアフローウォークスルー

### 4.1 単人承認フロー（SINGLE）

「張三が 1 日の年次休暇を申請」を例に：

| ステップ | インターフェース | データ処理 |
|------|------|----------|
| ① 張三ログイン | POST /api/login | JWT を取得；directLeaderId=1(王経理) |
| ② 休暇申請提出 | POST /api/leave/submit | LeaveRequest を作成、直属上司ノード（SINGLE）に進む、currentApproverId=1 |
| ③ 王経理が承認待ちを確認 | GET /api/leave/pending | WHERE current_approver_id=1 AND status="PENDING" |
| ④ 王経理が承認 | POST /api/leave/approve | requestId=1, action=APPROVE → 次のノードへ進む |
| ⑤ ③④を繰り返し | — | ノードがまだあればフロー継続、なければ APPROVED |

フローイメージ：

```
張三が提出 → 直属上司（王経理）→ 条件判定 → 部門長（条件を満たせば）→ 完了 ✓
```

### 4.2 会簽フロー（COUNTER_SIGN）

「張三が 5 日間の年次休暇を申請、2 名の総監による会簽が必要」を例に：

| ステップ | 説明 |
|------|------|
| ① 提出 | 会簽ノードに入り、2 件の approval_task を挿入（張総監、李総監）、currentApproverId=null |
| ② 張総監が承認 | 自分の task を完了、pendingCount>0 を確認 → 待機 |
| ③ 李総監が承認 | 自分の task を完了、pendingCount=0 → 次のノードへ進む |

### 4.3 或簽フロー（OR_SIGN）

いずれか1人が通過すれば進行：

| ステップ | 説明 |
|------|------|
| ① 提出 | 或簽ノードに入り、N 件の approval_task を挿入 |
| ② いずれかが承認 | 自分の task を完了、残りの task → SKIPPED、即座に進行 |

### 4.4 タイムアウト自動エスカレーション

1. ノードに `timeout_hours=24` を設定、ノード進入時に `timeout_time = now + 24h` を設定
2. `TimeoutScheduler` が 5 分ごとにスキャン：`WHERE status='PENDING' AND timeout_time <= now`
3. 設定された timeout_action を実行：
   - ESCALATE → 指定者に転送、またはスキップして次のノードへ
   - AUTO_APPROVE → 自動承認
   - AUTO_REJECT → 自動却下
4. `TIMEOUT_*` 承認ログを記録

---

## 五、設定説明

`application.properties` の主要設定：

```properties
# ========== データソース（MySQL） ==========
spring.datasource.url=jdbc:mysql://localhost:3306/smartoa
spring.datasource.username=root
spring.datasource.password=123456
# ========== MyBatis-Plus ==========
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
# ========== JWT ==========
jwt.secret=SmartOA-Base64...（256 ビット以上の秘密鍵）
jwt.expiration=86400000    ← 24 時間有効期限（ミリ秒）
# ========== CORS（WebConfig.java） ==========
http://localhost:5173 からのクロスオリジンを許可、GET/POST/PUT/DELETE/OPTIONS を許可
```

---

## 六、起動方法とテストアカウント

### 6.1 環境要件

- JDK 21+
- MySQL 8.0
- Node.js 18+ / pnpm
- Maven 3.8+

### 6.2 初回デプロイ手順

1. データベースを作成：
   ```sql
   CREATE DATABASE IF NOT EXISTS smartoa DEFAULT CHARACTER SET utf8mb4;
   ```
2. マイグレーションスクリプトを順に実行：
   ```
   docs/mysql-p0-upgrade.sql   — テーブル作成 + シードデータ
   docs/mysql-p1-upgrade.sql   — approval_node + template_field + 新フィールド
   docs/mysql-p3-bcrypt.sql    — BCrypt パスワード移行
   docs/mysql-p4-parallel.sql  — 並列承認（sign_type + approval_task）
   docs/mysql-p5-timeout.sql   — タイムアウト自動エスカレーション
   ```
3. バックエンドを起動：
   ```bash
   cd backend && ./mvnw spring-boot:run        # → localhost:8080
   ```
4. フロントエンドを起動：
   ```bash
   cd frontend
   pnpm install
   pnpm run dev                   # → localhost:5173
   ```

### 6.3 テストアカウント

| ユーザー名 | パスワード | 氏名 | 役割 | 用途 |
|--------|------|------|------|------|
| admin | 123456 | 王経理 | MANAGER | マネージャー承認 + テンプレート管理 |
| zhangsan | 123456 | 張三 | EMPLOYEE | 休暇申請提出 |
| zongjian1 | 123456 | 張総監 | MANAGER | 総監承認 |
| lisi | 123456 | 李四 | EMPLOYEE | 製品部社員 |
| zongjian2 | 123456 | 李総監 | MANAGER | 製品部総監 |

**推奨テストフロー：**

1. `zhangsan` でログイン → 休暇申請を 1 件提出
2. `admin` でログイン → 自分の承認 → 承認待ち → 承認
3. `zhangsan` でログイン → 申請状態を確認

---

## 七、プロジェクト構造

```
smartoa/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/                   # Result<T>、BusinessException、GlobalExceptionHandler
│   │   ├── config/                   # JWT 設定、CORS、フィルター
│   │   ├── controller/               # 5 個の REST コントローラー
│   │   ├── dto/                      # データ転送オブジェクト
│   │   ├── entity/                   # 7 個のエンティティクラス
│   │   ├── mapper/                   # 7 個の MyBatis-Plus Mapper
│   │   └── service/                  # 5 個の Service + TimeoutScheduler
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                  # auth.js / leave.js / template.js
│       ├── components/           # 6 個の共有コンポーネント
│       ├── layouts/              # AuthLayout / MainLayout
│       ├── router/               # index.js
│       ├── stores/               # auth.js / approval.js / users.js
│       ├── utils/                # constants.js
│       └── views/                # 15 個の Page コンポーネント
├── docs/                         # SQL マイグレーションスクリプト + 技術ドキュメント
├── CLAUDE.md                     # プロジェクト説明と開発進捗
└── README.md                     # プロジェクト README
```

---

— SmartOA P2 技術ドキュメント · 完 —
