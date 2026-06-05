# インターフェース設計ドキュメント

> <span style="color: #888888;"># 接口设计文档</span>

## SmartOA 承認フロー管理システム API 仕様

> <span style="color: #888888;">## SmartOA 审批流管理系统 API 规范</span>

---

## 1. 一般規約

> <span style="color: #888888;">## 1. 通用约定</span>

### 1.1 ベースパス

> <span style="color: #888888;">### 1.1 基础路径</span>

すべての API プレフィックス：`/api`

> <span style="color: #888888;">所有 API 前缀：`/api`</span>

### 1.2 認証方式

> <span style="color: #888888;">### 1.2 认证方式</span>

JWT Bearer Token、リクエストヘッダー：`Authorization: Bearer <token>`

> <span style="color: #888888;">JWT Bearer Token，请求头：`Authorization: Bearer <token>`</span>

ホワイトリストインターフェース（認証不要）：`/api/login`

> <span style="color: #888888;">白名单接口（无需认证）：`/api/login`</span>

### 1.3 レスポンス形式

> <span style="color: #888888;">### 1.3 响应格式</span>

統一して `Result<T>` 構造を使用する：

> <span style="color: #888888;">统一使用 `Result<T>` 结构：</span>

```json
// 成功（含数据）
{ "code": 200, "message": "操作成功", "data": { ... } }

// 成功（无数据）
{ "code": 200, "message": "操作成功", "data": null }

// 失败
{ "code": 500, "message": "错误原因", "data": null }
```

### 1.4 ステータスコード

> <span style="color: #888888;">### 1.4 状态码</span>

| HTTP 状态码 | code | 含义 |
|---|---|---|
| 200 | 200 | 请求成功 |
| 401 | 401 | 未登录或 Token 过期 |
| 403 | 403 | 无权限 |
| 400 | 400 | 请求参数有误 |
| 500 | 500 | 服务器内部错误 / 业务异常 |

---

## 2. 認証モジュール

> <span style="color: #888888;">## 2. 认证模块</span>

### 2.1 ログイン

> <span style="color: #888888;">### 2.1 登录</span>

```
POST /api/login
```

**リクエストボディ：**

> <span style="color: #888888;">**请求体：**</span>

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**レスポンス：**

> <span style="color: #888888;">**响应：**</span>

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOi...",
    "user": {
      "id": 2,
      "username": "zhangsan",
      "realName": "张三",
      "role": "EMPLOYEE",
      "department": "技术部",
      "directLeaderId": 1,
      "departmentHeadId": 4
    }
  }
}
```

### 2.2 現在のユーザーを取得

> <span style="color: #888888;">### 2.2 获取当前用户</span>

```
GET /api/user/current
```

### 2.3 ユーザー一覧を取得

> <span style="color: #888888;">### 2.3 获取用户列表</span>

```
GET /api/users
```

**レスポンス：** `Result<List<User>>`、password フィールドは `@JsonIgnore` で除外される

> <span style="color: #888888;">**响应：** `Result<List<User>>`，password 字段被 `@JsonIgnore` 排除</span>

### 2.4 ログアウト

> <span style="color: #888888;">### 2.4 登出</span>

```
POST /api/logout
```

---

## 3. 承認テンプレートモジュール

> <span style="color: #888888;">## 3. 审批模板模块</span>

### 3.1 テンプレート一覧

> <span style="color: #888888;">### 3.1 模板列表</span>

```
GET /api/templates
```

**レスポンス：** `Result<List<ApprovalTemplate>>`

> <span style="color: #888888;">**响应：** `Result<List<ApprovalTemplate>>`</span>

### 3.2 テンプレート詳細

> <span style="color: #888888;">### 3.2 模板详情</span>

```
GET /api/templates/{id}
```

### 3.3 テンプレート作成

> <span style="color: #888888;">### 3.3 创建模板</span>

```
POST /api/templates
```

**権限：** MANAGER

> <span style="color: #888888;">**权限：** MANAGER</span>

**リクエストボディ：**

> <span style="color: #888888;">**请求体：**</span>

```json
{
  "name": "请假申请",
  "description": "员工请假审批模板",
  "enabled": true
}
```

### 3.4 テンプレート更新

> <span style="color: #888888;">### 3.4 更新模板</span>

```
PUT /api/templates/{id}
```

**権限：** MANAGER

> <span style="color: #888888;">**权限：** MANAGER</span>

### 3.5 テンプレート削除

> <span style="color: #888888;">### 3.5 删除模板</span>

```
DELETE /api/templates/{id}
```

**権限：** MANAGER
**説明：** 関連する承認ノードとフォームフィールドをカスケード削除する

> <span style="color: #888888;">**权限：** MANAGER
**说明：** 级联删除关联的审批节点和表单字段</span>

### 3.6 テンプレートノード一覧

> <span style="color: #888888;">### 3.6 模板节点列表</span>

```
GET /api/templates/{id}/nodes
```

**レスポンス：** `Result<List<ApprovalNode>>`、`sortOrder` 昇順でソート

> <span style="color: #888888;">**响应：** `Result<List<ApprovalNode>>`，按 `sortOrder` 升序排列</span>

### 3.7 テンプレートノード保存

> <span style="color: #888888;">### 3.7 保存模板节点</span>

```
POST /api/templates/{id}/nodes
```

**権限：** MANAGER
**リクエストボディ：** `List<ApprovalNode>`
**説明：** 古いノードを先に削除し（参照をクリア）、その後新しいノードをバッチ挿入し、`sortOrder` を自動割り当てする

> <span style="color: #888888;">**权限：** MANAGER
**请求体：** `List<ApprovalNode>`
**说明：** 先删除旧节点（清理引用），再批量插入新节点，自动分配 `sortOrder`</span>

**ノードフィールド（P2 完全版）：**

> <span style="color: #888888;">**节点字段（P2 完整版）：**</span>

```json
{
  "nodeName": "部门总监审批",
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

> <span style="color: #888888;">### 3.8 删除节点</span>

```
DELETE /api/templates/{id}/nodes/{nodeId}
```

**権限：** MANAGER

> <span style="color: #888888;">**权限：** MANAGER</span>

### 3.9 テンプレートフィールド一覧

> <span style="color: #888888;">### 3.9 模板字段列表</span>

```
GET /api/templates/{id}/fields
```

**レスポンス：** `Result<List<TemplateField>>`

> <span style="color: #888888;">**响应：** `Result<List<TemplateField>>`</span>

---

## 4. 休暇申請モジュール

> <span style="color: #888888;">## 4. 请假申请模块</span>

### 4.1 申請提出

> <span style="color: #888888;">### 4.1 提交申请</span>

```
POST /api/leave/submit
```

**リクエストボディ：**

> <span style="color: #888888;">**请求体：**</span>

```json
{
  "templateId": 1,
  "leaveType": "年假",
  "startDate": "2026-06-01",
  "endDate": "2026-06-03",
  "reason": "回家探亲"
}
```

**説明：** システムが自動的にテンプレートノードを読み取り、条件分岐を評価し、承認者を解決し（SINGLE/並行）、timeoutTime を設定し、承認フローを開始する

> <span style="color: #888888;">**说明：** 系统自动读取模板节点，评估条件分支，解析审批人（SINGLE/并行），设置 timeoutTime，启动审批流程</span>

### 4.2 承認操作

> <span style="color: #888888;">### 4.2 审批操作</span>

```
POST /api/leave/approve
```

**リクエストボディ：**

> <span style="color: #888888;">**请求体：**</span>

```json
{
  "requestId": 1,
  "action": "APPROVE",
  "comment": "同意请假"
}
```

**action の値：** `APPROVE` | `REJECT`

> <span style="color: #888888;">**action 取值：** `APPROVE` | `REJECT`</span>

**SINGLE モード：** currentApproverId を検証 → 進行/終了
**並行モード：** approval_task を検索 → COUNTER_SIGN は全員同意後に進行 / OR_SIGN は任意の一人が同意で進行
**REJECT：** フローを終了 + 他の並行タスクをスキップ

> <span style="color: #888888;">**SINGLE 模式：** 校验 currentApproverId → 推进/终止
**并行模式：** 查 approval_task → COUNTER_SIGN 全部同意后推进 / OR_SIGN 任一人同意即推进
**REJECT：** 终止流程 + 跳过其他并行任务</span>

### 4.3 申請撤回

> <span style="color: #888888;">### 4.3 撤回申请</span>

```
POST /api/leave/{id}/withdraw
```

**権限：** 申請者のみ
**前提条件：** ステータスが PENDING
**説明：** ステータス → WITHDRAWN、並行タスクをスキップ、currentApproverId/timeoutTime をクリア

> <span style="color: #888888;">**权限：** 仅申请人
**前置条件：** 状态为 PENDING
**说明：** 状态 → WITHDRAWN，跳过并行任务，清除 currentApproverId/timeoutTime</span>

### 4.4 承認の転派

> <span style="color: #888888;">### 4.4 转派审批</span>

```
POST /api/leave/{id}/transfer
```

**権限：** 現在の承認者（SINGLE モードのみ）
**制約：** 並行承認ノードは転派に対応しない

> <span style="color: #888888;">**权限：** 当前审批人（仅 SINGLE 模式）
**约束：** 并行审批节点不支持转派</span>

**リクエストボディ：**

> <span style="color: #888888;">**请求体：**</span>

```json
{
  "toUserId": 3
}
```

### 4.5 自分の申請一覧

> <span style="color: #888888;">### 4.5 我的申请</span>

```
GET /api/leave/my-requests
```

### 4.6 承認待ち一覧

> <span style="color: #888888;">### 4.6 待审批列表</span>

```
GET /api/leave/pending
```

**説明：** `currentApproverId = 現在のユーザー` と `approval_task` 内の PENDING タスクを同時にマッチングする

> <span style="color: #888888;">**说明：** 同时匹配 `currentApproverId = 当前用户` 和 `approval_task` 中 PENDING 任务</span>

### 4.7 処理済み一覧

> <span style="color: #888888;">### 4.7 已处理列表</span>

```
GET /api/leave/done
```

### 4.8 申請詳細

> <span style="color: #888888;">### 4.8 申请详情</span>

```
GET /api/leave/{id}
```

**説明：** timeoutTime、ノード設定などの完全な情報を含む

> <span style="color: #888888;">**说明：** 含 timeoutTime、节点配置等完整信息</span>

### 4.9 承認記録

> <span style="color: #888888;">### 4.9 审批记录</span>

```
GET /api/leave/{id}/records
```

**レスポンス：** `Result<List<ApprovalRecord>>`、TIMEOUT_* のシステム自動操作記録を含む

> <span style="color: #888888;">**响应：** `Result<List<ApprovalRecord>>`，含 TIMEOUT_* 系统自动操作记录</span>

### 4.10 並行承認タスク照会（P2 新規）

> <span style="color: #888888;">### 4.10 并行审批任务查询（P2 新增）</span>

```
GET /api/leave/{id}/tasks
```

**レスポンス：** `Result<List<ApprovalTask>>`、現在のノードの PENDING 状態の承認タスク

> <span style="color: #888888;">**响应：** `Result<List<ApprovalTask>>`，当前节点 PENDING 状态的审批任务</span>

### 4.11 滞留修復（P2 新規）

> <span style="color: #888888;">### 4.11 滞留修复（P2 新增）</span>

```
POST /api/leave/repair
```

**説明：** `currentApproverId = null` かつ関連する PENDING タスクがない滞留申請を修復する

> <span style="color: #888888;">**说明：** 修复 `currentApproverId = null` 且无关联 PENDING 任务的滞留申请</span>

---

## 5. 統計モジュール

> <span style="color: #888888;">## 5. 统计模块</span>

### 5.1 統計サマリー

> <span style="color: #888888;">### 5.1 统计摘要</span>

```
GET /api/stats/summary
```

**レスポンス：**

> <span style="color: #888888;">**响应：**</span>

```json
{
  "code": 200,
  "data": {
    "avgDurations": [
      { "templateId": 1, "templateName": "请假申请", "avgMinutes": 124.5, "count": 15 }
    ],
    "templateUsages": [
      { "templateId": 1, "templateName": "请假申请", "count": 25 }
    ]
  }
}
```

---

## 6. エクスポートモジュール

> <span style="color: #888888;">## 6. 导出模块</span>

### 6.1 休暇申請 Excel エクスポート

> <span style="color: #888888;">### 6.1 导出请假单 Excel</span>

```
GET /api/stats/export
```

**権限：** MANAGER
**レスポンス：** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` バイナリストリーム
**ファイル名：** `请假单导出_2026-05-27.xlsx`

> <span style="color: #888888;">**权限：** MANAGER
**响应：** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 二进制流
**文件名：** `请假单导出_2026-05-27.xlsx`</span>

---

## 7. データモデル

> <span style="color: #888888;">## 7. 数据模型</span>

### 7.1 ApprovalNode（承認ノード — P2 完全版）

> <span style="color: #888888;">### 7.1 ApprovalNode（审批节点 — P2 完整版）</span>

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| templateId | BIGINT | 关联模板 ID |
| nodeName | VARCHAR(100) | 节点名称 |
| sortOrder | INT | 排序号，0 起 |
| approverType | VARCHAR(30) | 审批人类型 |
| approverId | BIGINT | 指定审批人 ID |
| signType | VARCHAR(20) | 签批模式：SINGLE/COUNTER_SIGN/OR_SIGN |
| approverIds | VARCHAR(1000) | 并行审批人 ID 列表（逗号分隔） |
| conditionExpression | VARCHAR(500) | SpEL 条件表达式 |
| timeoutHours | INT | 超时小时数 |
| timeoutAction | VARCHAR(30) | 超时动作：ESCALATE/AUTO_APPROVE/AUTO_REJECT |
| escalateToUserId | BIGINT | 超时转派目标用户 ID |

### 7.2 ApprovalTask（並行承認タスク）

> <span style="color: #888888;">### 7.2 ApprovalTask（并行审批任务）</span>

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| leaveRequestId | BIGINT | 关联申请 ID |
| nodeId | BIGINT | 关联节点 ID |
| approverId | BIGINT | 审批人 ID |
| status | VARCHAR(20) | PENDING / COMPLETED / SKIPPED |

---

## 8. インターフェース一覧

> <span style="color: #888888;">## 8. 接口汇总</span>

| 模块 | 数量 | 关键路径 |
|---|---|---|
| 认证 | 4 | /login, /user/current, /users, /logout |
| 模板 | 9 | /templates CRUD, /templates/{id}/nodes, /templates/{id}/fields |
| 请假 | 11 | /leave/submit, /leave/approve, /leave/{id}/withdraw, /leave/{id}/transfer, /leave/my-requests, /leave/pending, /leave/done, /leave/{id}, /leave/{id}/records, /leave/{id}/tasks, /leave/repair |
| 统计 | 1 | /stats/summary |
| 导出 | 1 | /stats/export |
| **合计** | **26** | |

---

> <span style="color: #888888;">> 文档版本：v2.0（P2 完成） | 更新日期：2026-05-27</span>

> ドキュメントバージョン：v2.0（P2 完了） | 更新日：2026-05-27