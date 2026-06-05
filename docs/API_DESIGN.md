Here is the complete translated file:

```markdown
# インターフェース設計ドキュメント

## SmartOA 承認フロー管理システム API 仕様

---

## 1. 一般規約

### 1.1 ベースパス

すべての API プレフィックス：`/api`

### 1.2 認証方式

JWT Bearer Token、リクエストヘッダー：`Authorization: Bearer <token>`

ホワイトリストインターフェース（認証不要）：`/api/login`

### 1.3 レスポンス形式

統一して `Result<T>` 構造を使用：

```json
// 成功（データあり）
{ "code": 200, "message": "操作成功", "data": { ... } }

// 成功（データなし）
{ "code": 200, "message": "操作成功", "data": null }

// 失敗
{ "code": 500, "message": "エラー原因", "data": null }
```

### 1.4 ステータスコード

| HTTP ステータスコード | code | 意味 |
|---|---|---|
| 200 | 200 | リクエスト成功 |
| 401 | 401 | 未ログインまたはトークン期限切れ |
| 403 | 403 | 権限なし |
| 400 | 400 | リクエストパラメータ不正 |
| 500 | 500 | サーバー内部エラー / ビジネス例外 |

---

## 2. 認証モジュール

### 2.1 ログイン

```
POST /api/login
```

**リクエストボディ：**

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**レスポンス：**

```json
{
  "code": 200,
  "message": "ログイン成功",
  "data": {
    "token": "eyJhbGciOi...",
    "user": {
      "id": 2,
      "username": "zhangsan",
      "realName": "張三",
      "role": "EMPLOYEE",
      "department": "技術部",
      "directLeaderId": 1,
      "departmentHeadId": 4
    }
  }
}
```

### 2.2 現在のユーザーを取得

```
GET /api/user/current
```

### 2.3 ユーザー一覧を取得

```
GET /api/users
```

**レスポンス：** `Result<List<User>>`、password フィールドは `@JsonIgnore` で除外

### 2.4 ログアウト

```
POST /api/logout
```

---

## 3. 承認テンプレートモジュール

### 3.1 テンプレート一覧

```
GET /api/templates
```

**レスポンス：** `Result<List<ApprovalTemplate>>`

### 3.2 テンプレート詳細

```
GET /api/templates/{id}
```

### 3.3 テンプレート作成

```
POST /api/templates
```

**権限：** MANAGER

**リクエストボディ：**

```json
{
  "name": "休暇申請",
  "description": "従業員休暇承認テンプレート",
  "enabled": true
}
```

### 3.4 テンプレート更新

```
PUT /api/templates/{id}
```

**権限：** MANAGER

### 3.5 テンプレート削除

```
DELETE /api/templates/{id}
```

**権限：** MANAGER
**説明：** 関連する承認ノードとフォームフィールドをカスケード削除

### 3.6 テンプレートノード一覧

```
GET /api/templates/{id}/nodes
```

**レスポンス：** `Result<List<ApprovalNode>>`、`sortOrder` 昇順で並べる

### 3.7 テンプレートノード保存

```
POST /api/templates/{id}/nodes
```

**権限：** MANAGER
**リクエストボディ：** `List<ApprovalNode>`
**説明：** 古いノードを削除（参照をクリア）してから、新しいノードをバッチ挿入し、自動的に `sortOrder` を割り当てる

**ノードフィールド（P2 完全版）：**

```json
{
  "nodeName": "部門ディレクター承認",
  "approverType": "DEPARTMENT_HEAD",
  "signType": "SINGLE",
  "approverIds": "2,3",
  "conditionExpression": "days > 3",
  "timeoutHours": 48,
  "timeoutAction": "ESCALATE",
  "escalateToUserId": 4
}
```

### 3.8 ノード削除

```
DELETE /api/templates/{id}/nodes/{nodeId}
```

**権限：** MANAGER

### 3.9 テンプレートフィールド一覧

```
GET /api/templates/{id}/fields
```

**レスポンス：** `Result<List<TemplateField>>`

---

## 4. 休暇申請モジュール

### 4.1 申請提出

```
POST /api/leave/submit
```

**リクエストボディ：**

```json
{
  "templateId": 1,
  "leaveType": "年次休暇",
  "startDate": "2026-06-01",
  "endDate": "2026-06-03",
  "reason": "帰省"
}
```

**説明：** システムは自動的にテンプレートノードを読み取り、条件分岐を評価し、承認者を解決し（SINGLE/並行）、timeoutTime を設定し、承認フローを開始する

### 4.2 承認操作

```
POST /api/leave/approve
```

**リクエストボディ：**

```json
{
  "requestId": 1,
  "action": "APPROVE",
  "comment": "休暇を承認"
}
```

**action の値：** `APPROVE` | `REJECT`

**SINGLE モード：** currentApproverId を検証 → 進行/終了
**並行モード：** approval_task を参照 → COUNTER_SIGN 全員同意後に進行 / OR_SIGN いずれか一人が同意すれば進行
**REJECT：** フローを終了 + 他の並行タスクをスキップ

### 4.3 申請取下げ

```
POST /api/leave/{id}/withdraw
```

**権限：** 申請者のみ
**前提条件：** ステータスが PENDING
**説明：** ステータス → WITHDRAWN、並行タスクをスキップ、currentApproverId/timeoutTime をクリア

### 4.4 承認転送

```
POST /api/leave/{id}/transfer
```

**権限：** 現在の承認者（SINGLE モードのみ）
**制約：** 並行承認ノードは転送をサポートしない

**リクエストボディ：**

```json
{
  "toUserId": 3
}
```

### 4.5 自分の申請

```
GET /api/leave/my-requests
```

### 4.6 承認待ち一覧

```
GET /api/leave/pending
```

**説明：** `currentApproverId = 現在のユーザー` と `approval_task` 内の PENDING タスクを同時にマッチ

### 4.7 処理済み一覧

```
GET /api/leave/done
```

### 4.8 申請詳細

```
GET /api/leave/{id}
```

**説明：** timeoutTime、ノード設定などの完全な情報を含む

### 4.9 承認記録

```
GET /api/leave/{id}/records
```

**レスポンス：** `Result<List<ApprovalRecord>>`、TIMEOUT_* システム自動操作記録を含む

### 4.10 並行承認タスク照会（P2 新規）

```
GET /api/leave/{id}/tasks
```

**レスポンス：** `Result<List<ApprovalTask>>`、現在のノードの PENDING 状態の承認タスク

### 4.11 滞留修復（P2 新規）

```
POST /api/leave/repair
```

**説明：** `currentApproverId = null` かつ関連する PENDING タスクがない滞留申請を修復する

---

## 5. 統計モジュール

### 5.1 統計サマリー

```
GET /api/stats/summary
```

**レスポンス：**

```json
{
  "code": 200,
  "data": {
    "avgDurations": [
      { "templateId": 1, "templateName": "休暇申請", "avgMinutes": 124.5, "count": 15 }
    ],
    "templateUsages": [
      { "templateId": 1, "templateName": "休暇申請", "count": 25 }
    ]
  }
}
```

---

## 6. エクスポートモジュール

### 6.1 休暇申請 Excel エクスポート

```
GET /api/stats/export
```

**権限：** MANAGER
**レスポンス：** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` バイナリストリーム
**ファイル名：** `休暇申請エクスポート_2026-05-27.xlsx`

---

## 7. データモデル

### 7.1 ApprovalNode（承認ノード — P2 完全版）

| フィールド | 型 | 説明 |
|---|---|---|
| id | BIGINT | 主キー |
| templateId | BIGINT | 関連テンプレート ID |
| nodeName | VARCHAR(100) | ノード名 |
| sortOrder | INT | ソート番号、0 から |
| approverType | VARCHAR(30) | 承認者タイプ |
| approverId | BIGINT | 指定承認者 ID |
| signType | VARCHAR(20) | 署名モード：SINGLE/COUNTER_SIGN/OR_SIGN |
| approverIds | VARCHAR(1000) | 並行承認者 ID リスト（カンマ区切り） |
| conditionExpression | VARCHAR(500) | SpEL 条件式 |
| timeoutHours | INT | タイムアウト時間数 |
| timeoutAction | VARCHAR(30) | タイムアウトアクション：ESCALATE/AUTO_APPROVE/AUTO_REJECT |
| escalateToUserId | BIGINT | タイムアウト転送先ユーザー ID |

### 7.2 ApprovalTask（並行承認タスク）

| フィールド | 型 | 説明 |
|---|---|---|
| id | BIGINT | 主キー |
| leaveRequestId | BIGINT | 関連申請 ID |
| nodeId | BIGINT | 関連ノード ID |
| approverId | BIGINT | 承認者 ID |
| status | VARCHAR(20) | PENDING / COMPLETED / SKIPPED |

---

## 8. インターフェース一覧

| モジュール | 数 | キーパス |
|---|---|---|
| 認証 | 4 | /login, /user/current, /users, /logout |
| テンプレート | 9 | /templates CRUD, /templates/{id}/nodes, /templates/{id}/fields |
| 休暇 | 11 | /leave/submit, /leave/approve, /leave/{id}/withdraw, /leave/{id}/transfer, /leave/my-requests, /leave/pending, /leave/done, /leave/{id}, /leave/{id}/records, /leave/{id}/tasks, /leave/repair |
| 統計 | 1 | /stats/summary |
| エクスポート | 1 | /stats/export |
| **合計** | **26** | |

---

ドキュメントバージョン：v2.0（P2 完了） | 更新日：2026-05-27
```