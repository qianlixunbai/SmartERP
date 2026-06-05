The complete bilingual markdown file has been written and verified at:

**`D:\claude聊天\SmartOA技术文档_bilingual.md`** (1140 lines)

Below is the complete translated content:

---

# SmartOA シンプル承認フロー管理システム -- 完全技術文書

> # SmartOA 简易审批流管理系统 — 完整技术文档

**バージョン：P2 完成版** | 日付：2026-05-27 | 技術スタック：Spring Boot 3.5.14 + Vue 3 + MySQL 8.0 + JWT + MyBatis-Plus

> **版本：P2 完成版** | 日期：2026-05-27 | 技术栈：Spring Boot 3.5.14 + Vue 3 + MySQL 8.0 + JWT + MyBatis-Plus

---

## 一、データベース設計

> ## 一、数据库设计

システムは全 8 テーブル、MySQL 8.0、文字セット utf8mb4、ストレージエンジン InnoDB。

> 系统共 8 张表，MySQL 8.0，字符集 utf8mb4，存储引擎 InnoDB。

### 1.1 ユーザーテーブル（sys_user）

> ### 1.1 用户表（sys_user）

ユーザー情報を保存し、組織階層関係（直属上司 + 部門ディレクター）を含む。

> 存储用户信息，含组织层级关系（直属领导 + 部门总监）。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | BCrypt 加密 |
| real_name | VARCHAR(50) | 真实姓名 |
| role | VARCHAR(20) | 角色：MANAGER / EMPLOYEE |
| department | VARCHAR(50) | 部门 |
| direct_leader_id | BIGINT | 直属领导 ID（自引用） |
| department_head_id | BIGINT | 部门总监 ID（自引用） |

**シードデータ**（全ユーザーパスワード：123456、BCrypt 暗号化）：

> **种子数据**（所有用户密码：123456，BCrypt 加密）：

| ID | 用户名 | 姓名 | 角色 | 部门 | 直属领导 | 部门总监 |
|----|--------|------|------|------|----------|----------|
| 1 | admin | 王经理 | MANAGER | 技术部 | 无 | 4(张总监) |
| 2 | zhangsan | 张三 | EMPLOYEE | 技术部 | 1(王经理) | 4(张总监) |
| 3 | lisi | 李四 | EMPLOYEE | 产品部 | 1(王经理) | 5(李总监) |
| 4 | zongjian1 | 张总监 | MANAGER | 技术部 | 无 | 无 |
| 5 | zongjian2 | 李总监 | MANAGER | 产品部 | 无 | 无 |

### 1.2 承認テンプレートテーブル（approval_template）

> ### 1.2 审批模板表（approval_template）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| name | VARCHAR(100) | 模板名称 |
| description | VARCHAR(500) | 模板描述 |
| enabled | BIT | 是否启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 1.3 承認ノードテーブル（approval_node）⭐ コア

> ### 1.3 审批节点表（approval_node）⭐ 核心

テンプレート配下の承認ステップで、`approval_template` から外部キー `template_id` で 1:N 関連付け。条件分岐、並行承認、タイムアウト設定をサポート。

> 模板下挂的审批步骤，由 `approval_template` 通过外键 `template_id` 1:N 关联。支持条件分支、并行审批、超时配置。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| template_id | BIGINT FK | 所属模板 ID |
| node_name | VARCHAR(100) | 节点名称 |
| sort_order | INT | 排序序号 |
| approver_type | VARCHAR(20) | DIRECT_LEADER / DEPARTMENT_HEAD / SPECIFIC_USER |
| approver_id | BIGINT | SPECIFIC_USER 时指定的用户 ID |
| condition_expression | VARCHAR(500) | SpEL 条件表达式（可空，如 `days > 3`） |
| sign_type | VARCHAR(20) | SINGLE（单人）/ COUNTER_SIGN（会签）/ OR_SIGN（或签） |
| approver_ids | VARCHAR(1000) | 并行签批时审批人 ID 列表，逗号分隔 |
| timeout_hours | INT | 超时小时数，NULL=不启用 |
| timeout_action | VARCHAR(20) | ESCALATE / AUTO_APPROVE / AUTO_REJECT |
| escalate_to_user_id | BIGINT | 超时转派目标用户 ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

**条件式変数**：`leaveType`（休暇種類 String）、`days`（休暇日数 long）、`startDate`、`endDate`（LocalDate）

> **条件表达式变量**：`leaveType`（请假类型 String）、`days`（请假天数 long）、`startDate`、`endDate`（LocalDate）

例：`days > 3` → 3 日超の休暇はこのノードを通る；`leaveType == '病假'` → 病欠はこのノードを通る。

> 示例：`days > 3` → 超过 3 天的请假走此节点；`leaveType == '病假'` → 病假走此节点。

### 1.4 休暇申請テーブル（leave_request）

> ### 1.4 请假申请表（leave_request）

コアテーブル。`current_node_id` + `current_approver_id` + `timeout_time` が承認フロー全体を駆動する。

> 核心表。`current_node_id` + `current_approver_id` + `timeout_time` 驱动整个审批流转。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| applicant_id | BIGINT FK | 申请人 ID → sys_user.id |
| template_id | BIGINT FK | 使用的模板 ID |
| leave_type | VARCHAR(20) | 请假类型 |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| reason | VARCHAR(500) | 请假原因 |
| status | VARCHAR(20) | PENDING / APPROVED / REJECTED / WITHDRAWN |
| approval_step | INT | 当前审批步骤序号 |
| current_node_id | BIGINT | 当前审批节点 ID（驱动流转） |
| current_approver_id | BIGINT | 当前审批人 ID（SINGLE 模式，并行模式为 null） |
| timeout_time | DATETIME | 当前节点超时截止时间（可空） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 1.5 承認記録テーブル（approval_record）

> ### 1.5 审批记录表（approval_record）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| leave_request_id | BIGINT FK | 请假单 ID |
| approver_id | BIGINT FK | 审批人 ID（0=系统自动） |
| action | VARCHAR(20) | APPROVE / REJECT / WITHDRAW / TRANSFER / TIMEOUT_* |
| comment | VARCHAR(500) | 审批意见 |
| approval_step | INT | 审批步骤号 |
| node_id | BIGINT | 审批节点 ID |
| create_time | DATETIME | 审批时间 |

### 1.6 テンプレートフィールドテーブル（template_field）

> ### 1.6 模板字段表（template_field）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| template_id | BIGINT FK | 所属模板 ID |
| field_name | VARCHAR(50) | 字段名 |
| field_label | VARCHAR(50) | 字段标签 |
| field_type | VARCHAR(20) | TEXT / NUMBER / DATE / SELECT |
| sort_order | INT | 排序序号 |
| required | BIT | 是否必填 |
| options | VARCHAR(500) | 选项（JSON，SELECT 类型用） |

### 1.7 並行承認タスクテーブル（approval_task）⭐ P2 新規

> ### 1.7 并行审批任务表（approval_task）⭐ P2 新增

並行承認（会签/或签）モードにおいて、各承認者の承認状態を追跡する。

> 并行审批（会签/或签）模式下，跟踪每个审批人的审批状态。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| leave_request_id | BIGINT FK | 请假单 ID |
| node_id | BIGINT FK | 审批节点 ID |
| approver_id | BIGINT FK | 审批人 ID |
| status | VARCHAR(20) | PENDING / COMPLETED / SKIPPED |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

制約：`UNIQUE (leave_request_id, node_id, approver_id)`

> 约束：`UNIQUE (leave_request_id, node_id, approver_id)`

---

## 二、バックエンドコアロジック

> ## 二、后端核心逻辑

### 2.1 JWT + BCrypt 認証フロー

> ### 2.1 JWT + BCrypt 认证流程

関連ファイル：`JwtProperties.java`、`JwtUtil.java`、`UserContextHolder.java`、`JwtFilter.java`、`WebConfig.java`

> 涉及文件：`JwtProperties.java`、`JwtUtil.java`、`UserContextHolder.java`、`JwtFilter.java`、`WebConfig.java`

**ログインフロー：**

> **登录流程：**

1. ユーザーが `POST /api/login` でユーザー名とパスワードを送信
2. `UserService.login()` が `sys_user` テーブルを検索し、`BCryptPasswordEncoder.matches()` でパスワードを照合
3. `JwtUtil.generateToken()` が JWT を生成（sub=ユーザーID、claims に username+role を含む、24h 有効期限）
4. `Result<Map>` を返却：`{code:200, data:{token, user:{id, username, realName, role, department, directLeaderId, departmentHeadId}}}`

> 
> 1. 用户 `POST /api/login` 提交用户名密码
> 2. `UserService.login()` 查 `sys_user` 表，使用 `BCryptPasswordEncoder.matches()` 比对密码
> 3. `JwtUtil.generateToken()` 生成 JWT（sub=用户ID，claims 含 username+role，24h 过期）
> 4. 返回 `Result<Map>`：`{code:200, data:{token, user:{id, username, realName, role, department, directLeaderId, departmentHeadId}}}`
> 

**リクエスト認証フロー：**

> **请求认证流程：**

1. 各 `/api/*` リクエストは `JwtFilter.doFilterInternal()` を通過
2. `Authorization: Bearer xxx` ヘッダーからトークンを抽出
3. jjwt ライブラリで署名 + 有効期限を検証
4. userId を解析 → `UserContextHolder`（ThreadLocal）に保存
5. Controller は `UserService.getLoginUser()` で現在のユーザーを取得
6. リクエスト終了後、`finally` ブロックで ThreadLocal をクリアし、メモリリークを防止

> 
> 1. 每个 `/api/*` 请求经过 `JwtFilter.doFilterInternal()`
> 2. 从 `Authorization: Bearer xxx` 头部提取 token
> 3. jjwt 库验证签名 + 过期时间
> 4. 解析出 userId → 存入 `UserContextHolder`（ThreadLocal）
> 5. Controller 通过 `UserService.getLoginUser()` 获取当前用户
> 6. 请求结束后 `finally` 块清空 ThreadLocal，防止内存泄漏
> 

### 2.2 承認フローエンジン（LeaveService.java）⭐ コア

> ### 2.2 审批流转引擎（LeaveService.java）⭐ 核心

#### 休暇申請送信（submitLeave）

> #### 提交请假（submitLeave）

1. 申請者エンティティを取得（directLeaderId、departmentHeadId を含む）
2. LeaveRequest を作成：status=PENDING、approvalStep=0
3. `advanceToNextNode()` を呼び出して最初の条件を満たす承認ノードに進む
4. データベースに保存し、フロントエンドに返却

> 
> 1. 获取申请人实体（含 directLeaderId、departmentHeadId）
> 2. 创建 LeaveRequest：status=PENDING, approvalStep=0
> 3. 调用 `advanceToNextNode()` 推进到第一个满足条件的审批节点
> 4. 入库，返回给前端
> 

#### 承認操作（approveLeave）

> #### 审批操作（approveLeave）

```
検証フェーズ：
1. 休暇申請は存在するか？
2. ステータスは PENDING か？
3. 承認者検証：
   - SINGLE モード：承認者 == currentApproverId
   - 並行モード：承認者が approval_task テーブルに PENDING タスクを持つ
   - 両方満たさない → 拒否

承認ログを記録 → ApprovalRecord

フロー判断：
├── REJECT（却下）
│   ├── ステータス → REJECTED、currentApproverId/currentNodeId/timeoutTime をクリア
│   └── 並行ノード：残りの PENDING タスクをスキップ
│
├── APPROVE + OR_SIGN（或签）
│   ├── 現在の承認者タスクを完了
│   ├── ノードの残りの PENDING タスクをスキップ
│   └── 次のノードに進む
│
├── APPROVE + COUNTER_SIGN（会签）
│   ├── 現在の承認者タスクを完了
│   ├── 全員が完了したか確認
│   ├── はい → 次のノードに進む
│   └── いいえ → 他の承認を待つ
│
└── APPROVE + SINGLE（单人）
    └── 次のノードに進む

次のノードに進む（advanceToNextNode）：
1. テンプレートの全ノードをロード（sortOrder でソート）
2. currentNodeId 以降から走査開始
3. 各ノードに対して：
   a. SpEL 条件判定を実行 → 満たさなければスキップ
   b. 承認者を解決：
      - SINGLE → approverType に応じて解決（DIRECT_LEADER/DEPARTMENT_HEAD/SPECIFIC_USER）
      - 並行モード → approverIds カンマ区切りリストを解決
   c. 承認者を割り当て：
      - SINGLE → currentApproverId を設定
      - 並行モード → currentApproverId=null、approval_task をバッチ挿入
   d. タイムアウト期限を設定：
      - timeout_hours がある場合 → timeout_time = now + timeout_hours
      - それ以外 → timeout_time = null
   e. true を返す
4. ノードがなくなった → false を返す（フロー完了）
```

> 
> ```
> 校验阶段：
> 1. 请假单存在？
> 2. 状态是 PENDING？
> 3. 审批人验证：
>    - SINGLE 模式：审批人 == currentApproverId
>    - 并行模式：审批人在 approval_task 表中有 PENDING 任务
>    - 两者都不满足 → 拒绝
> 
> 记录审批日志 → ApprovalRecord
> 
> 流转判断：
> ├── REJECT（驳回）
> │   ├── 状态 → REJECTED，清除 currentApproverId/currentNodeId/timeoutTime
> │   └── 并行节点：跳过其余 PENDING 任务
> │
> ├── APPROVE + OR_SIGN（或签）
> │   ├── 完成当前审批人任务
> │   ├── 跳过节点其余 PENDING 任务
> │   └── 推进到下一节点
> │
> ├── APPROVE + COUNTER_SIGN（会签）
> │   ├── 完成当前审批人任务
> │   ├── 检查是否所有人已完成
> │   ├── 是 → 推进到下一节点
> │   └── 否 → 等待其他人审批
> │
> └── APPROVE + SINGLE（单人）
>     └── 推进到下一节点
> 
> 推进到下一节点（advanceToNextNode）：
> 1. 加载模板所有节点（按 sortOrder 排序）
> 2. 从 currentNodeId 之后开始遍历
> 3. 对每个节点：
>    a. 执行 SpEL 条件判断 → 不满足则跳过
>    b. 解析审批人：
>       - SINGLE → 按 approverType 解析（DIRECT_LEADER/DEPARTMENT_HEAD/SPECIFIC_USER）
>       - 并行模式 → 解析 approverIds 逗号分隔列表
>    c. 分配审批人：
>       - SINGLE → 设置 currentApproverId
>       - 并行模式 → currentApproverId=null，批量插入 approval_task
>    d. 设置超时期限：
>       - 如有 timeout_hours → timeout_time = now + timeout_hours
>       - 否则 → timeout_time = null
>    e. 返回 true
> 4. 无更多节点 → 返回 false（流程完成）
> ```
> 

#### その他の操作

> #### 其他操作

| 操作 | 方法 | 説明 |
|------|------|------|
| 撤回 | `withdrawLeave()` | 自分が PENDING の申請のみ撤回可能、現在ノードの PENDING タスクをスキップ |
| 転送 | `transferLeave()` | 単人ノードを別ユーザーに転送（並行ノードは転送禁止） |
| 滞留修復 | `repairStuckRequests()` | currentApproverId が null で PENDING タスクがない滞留申請を修復 |
| タイムアウトチェック | `checkTimeouts()` | timeout_time が期限切れの申請を検索し、タイムアウト処理を実行 |

> 
> | 操作 | 方法 | 说明 |
> |------|------|------|
> | 撤回 | `withdrawLeave()` | 只能撤回自己 PENDING 的申请，跳过当前节点 PENDING 任务 |
> | 转派 | `transferLeave()` | 单人节点转派给其他用户（并行节点禁止转派） |
> | 滞留修复 | `repairStuckRequests()` | 修复 currentApproverId 为 null 且无 PENDING 任务的滞留申请 |
> | 超时检查 | `checkTimeouts()` | 查询 timeout_time 过期的申请，执行超时动作 |
> 

#### タイムアウト動作

> #### 超时动作

| 動作 | 説明 |
|------|------|
| ESCALATE | 指定された人物に転送（escalate_to_user_id あり）またはスキップして次のノードへ |
| AUTO_APPROVE | 現在のノードのすべての保留中タスクをスキップし、自動承認で次のノードへ |
| AUTO_REJECT | 申請を却下し、フローを終了 |

> 
> | 动作 | 说明 |
> |------|------|
> | ESCALATE | 转派给指定人（有 escalate_to_user_id）或跳过进入下一节点 |
> | AUTO_APPROVE | 跳过当前节点所有待审批任务，自动通过进入下一节点 |
> | AUTO_REJECT | 驳回申请，终止流程 |
> 

### 2.3 スケジュールタスク（TimeoutScheduler.java）

> ### 2.3 定时任务（TimeoutScheduler.java）

```java
@Scheduled(fixedRate = 300000) // 每 5 分钟
public void checkTimeouts() {
    int count = leaveService.checkTimeouts();
    if (count > 0) log.info("processed {} timed-out approvals", count);
}
```

### 2.4 承認テンプレート管理（TemplateService.java）

> ### 2.4 审批模板管理（TemplateService.java）

- `listAll()` → 全テンプレートを検索
- `getById(id)` → 単一テンプレートを検索、存在しなければ BusinessException をスロー
- `create(template)` → テンプレートを作成
- `update(id, data)` → name/description/enabled を更新
- `delete(id)` → テンプレート・ノード・フィールドをカスケード削除
- `listNodes(templateId)` → テンプレートの承認ノードを検索（sortOrder でソート）
- `saveNodes(templateId, nodes)` → ノードを全量置換（旧ノードを削除、参照をクリーンアップし、新ノードを挿入）
- `listFields(templateId)` → テンプレートのカスタムフィールドを検索

> 
> - `listAll()` → 查询所有模板
> - `getById(id)` → 查询单个模板，不存在抛 BusinessException
> - `create(template)` → 创建模板
> - `update(id, data)` → 更新 name/description/enabled
> - `delete(id)` → 级联删除模板、节点、字段
> - `listNodes(templateId)` → 查询模板的审批节点（按 sortOrder 排序）
> - `saveNodes(templateId, nodes)` → 全量替换节点（先删旧节点，清理引用，再插新节点）
> - `listFields(templateId)` → 查询模板的自定义字段
> 

### 2.5 権限制御

> ### 2.5 权限控制

| モジュール | 権限方式 | 説明 |
|------|----------|------|
| テンプレート管理 | Controller 層 `role == "MANAGER"` | MANAGER 以外は 403 返却 |
| 承認操作 | Service 層 `currentApproverId` / `approval_task` | 承認者に指定された人は誰でも承認可能 |
| フロントエンドルート | `router.beforeEach` ロールガード | MANAGER 以外が /templates にアクセスするとホームにリダイレクト |
| CORS | WebConfig で localhost:5173 を許可 | 開発環境のクロスオリジン対応 |

> 
> | 模块 | 权限方式 | 说明 |
> |------|----------|------|
> | 模板管理 | Controller 层 `role == "MANAGER"` | 非 MANAGER 返回 403 |
> | 审批操作 | Service 层 `currentApproverId` / `approval_task` | 任何人被指定为审批人即可批 |
> | 前端路由 | `router.beforeEach` 角色守卫 | 非 MANAGER 访问 /templates 跳转主页 |
> | CORS | WebConfig 允许 localhost:5173 | 开发环境跨域支持 |
> 

### 2.6 統一レスポンス形式

> ### 2.6 统一响应格式

```json
// 成功
{ "code": 200, "message": "操作成功", "data": { ... } }

// 业务异常
{ "code": 500, "message": "错误原因", "data": null }

// 未登录
{ "code": 401, "message": "请先登录", "data": null }

// 无权限
{ "code": 403, "message": "无权限", "data": null }
```

`BusinessException` + `@RestControllerAdvice`（GlobalExceptionHandler）によるグローバル統一例外処理。

> `BusinessException` + `@RestControllerAdvice`（GlobalExceptionHandler）全局统一异常处理。

### 2.7 API 一覧（全 20+ エンドポイント）

> ### 2.7 接口清单（共 20+ 个）

| 序号 | 路径 | 方法 | 认证 | 説明 |
|------|------|------|------|------|
| 1 | `/api/login` | POST | 否 | ログイン |
| 2 | `/api/users` | GET | 是 | ユーザー一覧 |
| 3 | `/api/users/current` | GET | 是 | 現在のユーザーを取得 |
| 4 | `/api/leave/submit` | POST | 是 | 休暇申請を送信 |
| 5 | `/api/leave/approve` | POST | 是 | 休暇を承認 |
| 6 | `/api/leave/{id}/withdraw` | POST | 是 | 撤回 |
| 7 | `/api/leave/{id}/transfer` | POST | 是 | 転送 |
| 8 | `/api/leave/repair` | POST | 是 | 滞留申請を修復 |
| 9 | `/api/leave/all` | GET | 是 | 全申請（MANAGER） |
| 10 | `/api/leave/my-requests` | GET | 是 | 自分の申請 |
| 11 | `/api/leave/pending` | GET | 是 | 承認待ち一覧 |
| 12 | `/api/leave/done` | GET | 是 | 処理済み一覧 |
| 13 | `/api/leave/{id}` | GET | 是 | 休暇申請詳細 |
| 14 | `/api/leave/{id}/records` | GET | 是 | 承認記録 |
| 15 | `/api/leave/{id}/tasks` | GET | 是 | 並行承認タスク |
| 16 | `/api/templates` | GET | 是 | テンプレート一覧 |
| 17 | `/api/templates/{id}` | GET | 是 | テンプレート詳細 |
| 18 | `/api/templates` | POST | MANAGER | テンプレートを作成 |
| 19 | `/api/templates/{id}` | PUT | MANAGER | テンプレートを更新 |
| 20 | `/api/templates/{id}` | DELETE | MANAGER | テンプレートを削除 |
| 21 | `/api/templates/{id}/nodes` | GET | 是 | 承認ノードを取得 |
| 22 | `/api/templates/{id}/nodes` | POST | MANAGER | 承認ノードを保存 |
| 23 | `/api/templates/{id}/fields` | GET | 是 | テンプレートフィールドを取得 |
| 24 | `/api/stats/summary` | GET | 是 | 統計サマリー |
| 25 | `/api/stats/export` | GET | 是 | Excel エクスポート |

> 
> | 序号 | 路径 | 方法 | 认证 | 说明 |
> |------|------|------|------|------|
> | 1 | `/api/login` | POST | 否 | 登录 |
> | 2 | `/api/users` | GET | 是 | 用户列表 |
> | 3 | `/api/users/current` | GET | 是 | 获取当前用户 |
> | 4 | `/api/leave/submit` | POST | 是 | 提交请假申请 |
> | 5 | `/api/leave/approve` | POST | 是 | 审批请假 |
> | 6 | `/api/leave/{id}/withdraw` | POST | 是 | 撤回 |
> | 7 | `/api/leave/{id}/transfer` | POST | 是 | 转派 |
> | 8 | `/api/leave/repair` | POST | 是 | 修复滞留申请 |
> | 9 | `/api/leave/all` | GET | 是 | 全部申请（MANAGER） |
> | 10 | `/api/leave/my-requests` | GET | 是 | 我的申请 |
> | 11 | `/api/leave/pending` | GET | 是 | 待审批列表 |
> | 12 | `/api/leave/done` | GET | 是 | 已处理列表 |
> | 13 | `/api/leave/{id}` | GET | 是 | 请假单详情 |
> | 14 | `/api/leave/{id}/records` | GET | 是 | 审批记录 |
> | 15 | `/api/leave/{id}/tasks` | GET | 是 | 并行审批任务 |
> | 16 | `/api/templates` | GET | 是 | 模板列表 |
> | 17 | `/api/templates/{id}` | GET | 是 | 模板详情 |
> | 18 | `/api/templates` | POST | MANAGER | 创建模板 |
> | 19 | `/api/templates/{id}` | PUT | MANAGER | 更新模板 |
> | 20 | `/api/templates/{id}` | DELETE | MANAGER | 删除模板 |
> | 21 | `/api/templates/{id}/nodes` | GET | 是 | 获取审批节点 |
> | 22 | `/api/templates/{id}/nodes` | POST | MANAGER | 保存审批节点 |
> | 23 | `/api/templates/{id}/fields` | GET | 是 | 获取模板字段 |
> | 24 | `/api/stats/summary` | GET | 是 | 统计摘要 |
> | 25 | `/api/stats/export` | GET | 是 | Excel 导出 |
> 

---

## 三、フロントエンドアーキテクチャ

> ## 三、前端架构

### 3.1 技術スタック

> ### 3.1 技术栈

Vue 3.5（Composition API）+ Element Plus 2.13.7 + Vite 8 + Pinia + Vue Router 5 + Axios + ECharts 5

> Vue 3.5（Composition API）+ Element Plus 2.13.7 + Vite 8 + Pinia + Vue Router 5 + Axios + ECharts 5

エントリーファイル `main.js`：Vue アプリ作成 → Element Plus Icons 登録 → Pinia/Router/ElementPlus インストール → `#app` にマウント

> 入口文件 `main.js`：创建 Vue 应用 → 注册 Element Plus Icons → 安装 Pinia/Router/ElementPlus → 挂载 `#app`

### 3.2 レイアウトシステム

> ### 3.2 布局系统

| レイアウト | ファイル | 使用シーン | 構造 |
|------|------|----------|------|
| AuthLayout | `layouts/AuthLayout.vue` | /login | 全画面中央寄せ、紫色グラデーション背景 |
| MainLayout | `layouts/MainLayout.vue` | ログイン以外の全ページ | 左 220px サイドバー + 上 60px ナビ + 中央グレー背景コンテンツエリア |

> 
> | 布局 | 文件 | 使用场景 | 结构 |
> |------|------|----------|------|
> | AuthLayout | `layouts/AuthLayout.vue` | /login | 全屏居中，紫色渐变背景 |
> | MainLayout | `layouts/MainLayout.vue` | 除登录外所有页面 | 左 220px 侧边栏 + 上 60px 导航 + 中间灰底内容区 |
> 

### 3.3 ルート設定

> ### 3.3 路由配置

| パス | ページ | 認証 | ロール |
|------|------|------|------|
| `/login` | LoginPage.vue | ゲスト | — |
| `/submit-application` | SubmitApplicationPage.vue | 必須 | — |
| `/my-approvals` | MyApprovalsPage.vue | 必須 | — |
| `/approval/:id` | ApprovalDetailPage.vue | 必須 | — |
| `/templates` | TemplateListPage.vue | 必須 | MANAGER |
| `/templates/edit/:id?` | TemplateEditPage.vue | 必須 | MANAGER |
| `/stats` | StatsPage.vue | 必須 | — |
| `/` | リダイレクト | → /submit-application | — |
| `/:pathMatch(.*)*` | NotFoundPage.vue | — | 404 |

> 
> | 路径 | 页面 | 认证 | 角色 |
> |------|------|------|------|
> | `/login` | LoginPage.vue | 游客 | — |
> | `/submit-application` | SubmitApplicationPage.vue | 必须 | — |
> | `/my-approvals` | MyApprovalsPage.vue | 必须 | — |
> | `/approval/:id` | ApprovalDetailPage.vue | 必须 | — |
> | `/templates` | TemplateListPage.vue | 必须 | MANAGER |
> | `/templates/edit/:id?` | TemplateEditPage.vue | 必须 | MANAGER |
> | `/stats` | StatsPage.vue | 必须 | — |
> | `/` | 重定向 | → /submit-application | — |
> | `/:pathMatch(.*)*` | NotFoundPage.vue | — | 404 |
> 

**ルートガード（beforeEach）：**

> **路由守卫（beforeEach）：**

1. トークンありだがユーザーなし → 自動で `fetchUser()` を呼び出してログイン状態を復元
2. 認証必要だが未ログイン → `/login` にリダイレクト
3. ログイン済みで `/login` にアクセス → `/submit-application` にリダイレクト
4. ロール不一致 → `/submit-application` にリダイレクト

> 
> 1. 有 token 但无 user → 自动调 `fetchUser()` 恢复登录态
> 2. 需要认证但未登录 → 跳转 `/login`
> 3. 已登录访问 `/login` → 跳转 `/submit-application`
> 4. 角色不匹配 → 跳转 `/submit-application`
> 

### 3.4 Axios ラッパー（api/index.js）

> ### 3.4 Axios 封装（api/index.js）

- baseURL = `/api`（Vite 開発サーバーが localhost:8080 にプロキシ）
- リクエストインターセプター：自動で `Authorization: Bearer <token>` を付加
- レスポンスインターセプター：`code === 200` → `data` を直接返却；`code === 401` → トークンクリア + ログイン画面にリダイレクト；その他 → `ElMessage.error`

> 
> - baseURL = `/api`（Vite 开发服务器代理到 localhost:8080）
> - 请求拦截器：自动附加 `Authorization: Bearer <token>`
> - 响应拦截器：`code === 200` → 直接返回 `data`；`code === 401` → 清空 token + 跳转登录；其他 → `ElMessage.error`
> 

### 3.5 状態管理（Pinia）

> ### 3.5 状态管理（Pinia）

**auth store：**

> **auth store：**

| 状態/メソッド | 説明 |
|-----------|------|
| token | JWT トークン、localStorage に永続化 |
| user | ユーザー情報オブジェクト、localStorage に永続化 |
| isManager | 算出プロパティ：`user.role === "MANAGER"` |
| login() | ログインAPI呼出 → token+user保存 → ホーム画面に遷移 |
| fetchUser() | GET /api/users/current で現在のユーザーをリフレッシュ |
| logout() | 全状態をクリア → ログイン画面に遷移 |

> 
> | 状态/方法 | 说明 |
> |-----------|------|
> | token | JWT 令牌，持久化到 localStorage |
> | user | 用户信息对象，持久化到 localStorage |
> | isManager | 计算属性：`user.role === "MANAGER"` |
> | login() | 调登录接口 → 存 token+user → 跳转主页 |
> | fetchUser() | GET /api/users/current 刷新当前用户 |
> | logout() | 清空所有状态 → 跳转登录页 |
> 

**approval store：**

> **approval store：**

| 状態/メソッド | 説明 |
|-----------|------|
| pendingRequests / doneRequests / myRequests | 三種の承認リスト |
| currentDetail / currentRecords / currentTasks | 現在の休暇申請詳細/記録/並行タスク |
| fetchPendingRequests() | GET /api/leave/pending |
| fetchDoneRequests() | GET /api/leave/done |
| fetchDetail(id) | GET /api/leave/{id} |
| fetchRecords(id) | GET /api/leave/{id}/records |
| fetchTasks(id) | GET /api/leave/{id}/tasks |

> 
> | 状态/方法 | 说明 |
> |-----------|------|
> | pendingRequests / doneRequests / myRequests | 三种审批列表 |
> | currentDetail / currentRecords / currentTasks | 当前请假单详情/记录/并行任务 |
> | fetchPendingRequests() | GET /api/leave/pending |
> | fetchDoneRequests() | GET /api/leave/done |
> | fetchDetail(id) | GET /api/leave/{id} |
> | fetchRecords(id) | GET /api/leave/{id}/records |
> | fetchTasks(id) | GET /api/leave/{id}/tasks |
> 

### 3.6 ページコンポーネント詳細

> ### 3.6 页面组件详解

**1. ログインページ（LoginPage.vue）**
- 紫色グラデーション全画面背景、白い中央 400px カード
- ユーザー名 + パスワード入力欄（アイコン付き）
- Enter キーによるクイックログイン対応
- 下部にテストアカウントのヒント表示

> 
> **① 登录页（LoginPage.vue）**
> - 紫色渐变全屏背景，白色居中 400px 卡片
> - 用户名 + 密码输入框（带图标）
> - 支持回车键快速登录
> - 底部提示测试账号
> 

**2. 申請送信（SubmitApplicationPage.vue）**
- 2 カラムレイアウト：左側フォーム + 右側自分の休暇記録
- 休暇種類ドロップダウン（6 種類）+ 日付選択 + 理由テキストエリア
- フロントエンド検証：終了日 >= 開始日、必須項目の非空チェック
- 送信成功：通知 + フォームクリア + リスト更新

> 
> **② 提交申请（SubmitApplicationPage.vue）**
> - 双栏布局：左侧表单 + 右侧我的请假记录
> - 请假类型下拉（6 种）+ 日期选择器 + 原因文本域
> - 前端校验：结束日期 >= 开始日期，必填项非空
> - 提交成功：提示 + 清空表单 + 刷新列表
> 

**3. 自分の承認（MyApprovalsPage.vue）**
- 3 タブ構成：承認待ち / 処理済み / 自分の申請
- テーブル列：ID、申請者、休暇種類、日付、状態、操作

> 
> **③ 我的审批（MyApprovalsPage.vue）**
> - 三 Tab 架构：待审批 / 已处理 / 我的申请
> - 表格列：ID、申请人、请假类型、日期、状态、操作
> 

**4. 承認詳細（ApprovalDetailPage.vue）⭐ コアページ**
- 上部 `el-steps` ステップバー（動的ノード + 完了ステップ）
- 並行承認：現在のノードの全承認待ちユーザーラベルを表示
- タイムアウト警告：タイムアウト期限を表示（黄色警告バー）
- 詳細エリア `el-descriptions`（2 列、枠線付き）
- 承認操作エリア（複数人対応）：
  - SINGLE モード：`currentApproverId` 一致で表示
  - 並行モード：`approval_task` に PENDING タスクがある場合に表示
- 転送ボタン + ダイアログ（SINGLE モードのみ利用可能）
- 撤回ボタン（申請者のみ表示）
- 承認記録タイムライン

> 
> **④ 审批详情（ApprovalDetailPage.vue）⭐ 核心页面**
> - 顶部 `el-steps` 步骤条（动态节点 + 完成步骤）
> - 并行审批：显示当前节点所有待审批人标签
> - 超时提醒：显示超时截止时间（黄色警告栏）
> - 详情区 `el-descriptions`（2 列带边框）
> - 审批操作区（多人兼容）：
>   - SINGLE 模式：`currentApproverId` 匹配显示
>   - 并行模式：`approval_task` 中有 PENDING 任务显示
> - 转派按钮 + 弹窗（仅 SINGLE 模式可用）
> - 撤回按钮（仅申请人可见）
> - 审批记录时间线
> 

**5. テンプレート一覧（TemplateListPage.vue）**
- テーブルでテンプレート一覧を表示
- 新規作成/編集/削除操作（MANAGER のみ）

> 
> **⑤ 模板列表（TemplateListPage.vue）**
> - 表格展示模板列表
> - 新建/编辑/删除操作（MANAGER only）
> 

**6. テンプレート編集（TemplateEditPage.vue）⭐ P2 強化**
- ルートパラメータ `:id?` はオプション
- 基本情報フォーム：テンプレート名 + 説明 + 有効スイッチ
- **ビジュアルフローエディタ**：
  - ドラッグハンドルでノード並び替え
  - 各ノードカードに含まれるもの：ノード名 + 承認モード（単人/会签/或签）+ 承認者タイプ/複数人選択
  - 条件式（SpEL）：折りたたみ展開可能、例：`days > 3`
  - タイムアウト設定：折りたたみ展開可能、タイムアウト時間 + タイムアウト動作（転送/自動承認/自動却下）+ 対象ユーザー
  - 削除ボタン（ホバー時表示）
  - ノード間接続線

> 
> **⑥ 模板编辑（TemplateEditPage.vue）⭐ P2 增强**
> - 路由参数 `:id?` 可选
> - 基本信息表单：模板名称 + 描述 + 启用开关
> - **可视化流程编辑器**：
>   - 拖拽手柄排序节点
>   - 每个节点卡片包含：节点名称 + 签批模式（单人/会签/或签）+ 审批人类型/多人选择器
>   - 条件表达式（SpEL）：可折叠展开，如 `days > 3`
>   - 超时设置：可折叠展开，配超时小时数 + 超时动作（转派/自动通过/自动驳回）+ 目标用户
>   - 删除按钮（hover 显示）
>   - 节点间连接线
> 

**7. 統計ページ（StatsPage.vue）**
- ECharts グラフ：テンプレート平均承認時間 + 各テンプレート使用量

> 
> **⑦ 统计页（StatsPage.vue）**
> - ECharts 图表：模板平均审批时长 + 各模板使用量
> 

### 3.7 共有コンポーネント

> ### 3.7 共享组件

| コンポーネント | ファイル | 機能 |
|------|------|------|
| LeaveTable | `LeaveTable.vue` | 汎用承認テーブル |
| LeaveForm | `LeaveForm.vue` | 休暇フォームカプセル化 |
| ApprovalTimeline | `ApprovalTimeline.vue` | 承認タイムライン |
| StatusTag | `StatusTag.vue` | 状態タグ（PENDING=オレンジ/APPROVED=緑/REJECTED=赤/WITHDRAWN=グレー） |

> 
> | 组件 | 文件 | 功能 |
> |------|------|------|
> | LeaveTable | `LeaveTable.vue` | 通用审批表格 |
> | LeaveForm | `LeaveForm.vue` | 请假表单封装 |
> | ApprovalTimeline | `ApprovalTimeline.vue` | 审批时间线 |
> | StatusTag | `StatusTag.vue` | 状态标签（PENDING=橙色/APPROVED=绿色/REJECTED=红色/WITHDRAWN=灰色） |
> 

---

## 四、コアフローウォークスルー

> ## 四、核心流程走查

### 4.1 単人承認フロー（SINGLE）

> ### 4.1 单人审批流程（SINGLE）

「張三が 1 日の年次休暇を申請」を例に：

> 以"张三请 1 天年假"为例：

| ステップ | API | データ処理 |
|------|------|----------|
| 1. 張三ログイン | POST /api/login | JWT 取得；directLeaderId=1(王经理) |
| 2. 休暇申請送信 | POST /api/leave/submit | LeaveRequest 作成、直属上司ノード（SINGLE）に進む、currentApproverId=1 |
| 3. 王经理が保留中を確認 | GET /api/leave/pending | WHERE current_approver_id=1 AND status="PENDING" |
| 4. 王经理が承認 | POST /api/leave/approve | requestId=1, action=APPROVE → 次のノードに進む |
| 5. 3〜4 を繰り返し | — | ノードがまだあれば続行、なければ APPROVED |

> 
> | 步骤 | 接口 | 数据处理 |
> |------|------|----------|
> | ① 张三登录 | POST /api/login | 获取 JWT；directLeaderId=1(王经理) |
> | ② 提交请假 | POST /api/leave/submit | 创建 LeaveRequest，推进到直属领导节点（SINGLE），currentApproverId=1 |
> | ③ 王经理查看待办 | GET /api/leave/pending | WHERE current_approver_id=1 AND status="PENDING" |
> | ④ 王经理审批通过 | POST /api/leave/approve | requestId=1, action=APPROVE → 推进到下一节点 |
> | ⑤ 重复③④ | — | 如还有节点继续流转，否则 APPROVED |
> 

フロー図：

> 流转示意：

```
張三が申請 → 直属上司（王经理）→ 条件判定 → 部門ディレクター（条件を満たせば）→ 完了 ✓
```

> 
> ```
> 张三提交 → 直属领导（王经理）→ 条件判断 → 部门总监（如满足条件）→ 完成 ✓
> ```
> 

### 4.2 会签フロー（COUNTER_SIGN）

> ### 4.2 会签流程（COUNTER_SIGN）

「張三が 5 日の年次休暇を申請し、2 名のディレクターの会签が必要」を例に：

> 以"张三请 5 天年假，需 2 位总监会签"为例：

| ステップ | 説明 |
|------|------|
| 1. 送信 | 会签ノードに入り、2 件の approval_task を挿入（张总监、李总监）、currentApproverId=null |
| 2. 张总监が承認 | 自分の task を完了、pendingCount>0 を確認 → 待機 |
| 3. 李总监が承認 | 自分の task を完了、pendingCount=0 → 次のノードに進む |

> 
> | 步骤 | 说明 |
> |------|------|
> | ① 提交 | 进入会签节点，插入 2 条 approval_task（张总监、李总监），currentApproverId=null |
> | ② 张总监审批通过 | 完成自己的 task，检查 pendingCount>0 → 等待 |
> | ③ 李总监审批通过 | 完成自己的 task，pendingCount=0 → 推进到下一节点 |
> 

### 4.3 或签フロー（OR_SIGN）

> ### 4.3 或签流程（OR_SIGN）

誰か 1 人が通過すれば次に進む：

> 任何一人通过即推进：

| ステップ | 説明 |
|------|------|
| 1. 送信 | 或签ノードに入り、N 件の approval_task を挿入 |
| 2. 任意の 1 人が承認 | 自分の task を完了、残りの task → SKIPPED、即座に次のノードへ |

> 
> | 步骤 | 说明 |
> |------|------|
> | ① 提交 | 进入或签节点，插入 N 条 approval_task |
> | ② 任一人审批通过 | 完成自己的 task，其余 task → SKIPPED，立即推进 |
> 

### 4.4 タイムアウト自動エスカレーション

> ### 4.4 超时自动升级

1. ノード設定 `timeout_hours=24`、ノード進入時に `timeout_time = now + 24h` を設定
2. `TimeoutScheduler` が 5 分ごとにスキャン：`WHERE status='PENDING' AND timeout_time <= now`
3. 設定された timeout_action を実行：
   - ESCALATE → 指定された人物に転送、またはスキップして次のノードへ
   - AUTO_APPROVE → 自動承認
   - AUTO_REJECT → 自動却下
4. `TIMEOUT_*` 承認ログを記録

> 
> 1. 节点配置 `timeout_hours=24`，进入节点时设置 `timeout_time = now + 24h`
> 2. `TimeoutScheduler` 每 5 分钟扫描：`WHERE status='PENDING' AND timeout_time <= now`
> 3. 执行配置的 timeout_action：
>    - ESCALATE → 转派给指定人或跳过进入下一节点
>    - AUTO_APPROVE → 自动通过
>    - AUTO_REJECT → 自动驳回
> 4. 记录 `TIMEOUT_*` 审批日志
> 

---

## 五、設定説明

> ## 五、配置说明

`application.properties` 主要設定：

> `application.properties` 关键配置：

```properties
# ========== データソース（MySQL） ==========
spring.datasource.url=jdbc:mysql://localhost:3306/smartoa
spring.datasource.username=root
spring.datasource.password=123456

# ========== MyBatis-Plus ==========
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

# ========== JWT ==========
jwt.secret=SmartOA-Base64...（最低 256 ビット鍵）
jwt.expiration=86400000    ← 24 時間有効期限（ミリ秒）

# ========== CORS（WebConfig.java） ==========
http://localhost:5173 のクロスオリジンを許可、GET/POST/PUT/DELETE/OPTIONS を許可
```

> 
> ```properties
> # ========== 数据源（MySQL） ==========
> spring.datasource.url=jdbc:mysql://localhost:3306/smartoa
> spring.datasource.username=root
> spring.datasource.password=123456
> 
> # ========== MyBatis-Plus ==========
> mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
> 
> # ========== JWT ==========
> jwt.secret=SmartOA-Base64...（至少 256 位密钥）
> jwt.expiration=86400000    ← 24 小时过期（毫秒）
> 
> # ========== CORS（WebConfig.java） ==========
> 允许 http://localhost:5173 跨域，允许 GET/POST/PUT/DELETE/OPTIONS
> ```
> 

---

## 六、起動方法とテストアカウント

> ## 六、启动方式与测试账号

### 6.1 環境要件

> ### 6.1 环境要求

- JDK 21+
- MySQL 8.0
- Node.js 18+ / pnpm
- Maven 3.8+

> 
> - JDK 21+
> - MySQL 8.0
> - Node.js 18+ / pnpm
> - Maven 3.8+
> 

### 6.2 初回デプロイ手順

> ### 6.2 首次部署步骤

1. データベースを作成：
   ```sql
   CREATE DATABASE IF NOT EXISTS smartoa DEFAULT CHARACTER SET utf8mb4;
   ```

2. マイグレーションスクリプトを順に実行：
   ```
   docs/mysql-p0-upgrade.sql   — テーブル作成 + シードデータ
   docs/mysql-p3-bcrypt.sql    — BCrypt パスワード移行
   docs/mysql-p4-parallel.sql  — 並行承認（sign_type + approval_task）
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

> 
> 1. 创建数据库：
>    ```sql
>    CREATE DATABASE IF NOT EXISTS smartoa DEFAULT CHARACTER SET utf8mb4;
>    ```
> 
> 2. 依次执行迁移脚本：
>    ```
>    docs/mysql-p0-upgrade.sql   — 建表 + 种子数据
>    docs/mysql-p3-bcrypt.sql    — BCrypt 密码迁移
>    docs/mysql-p4-parallel.sql  — 并行审批（sign_type + approval_task）
>    docs/mysql-p5-timeout.sql   — 超时自动升级
>    ```
> 
> 3. 启动后端：
>    ```bash
>    cd backend && ./mvnw spring-boot:run        # → localhost:8080
>    ```
> 
> 4. 启动前端：
>    ```bash
>    cd frontend
>    pnpm install
>    pnpm run dev                   # → localhost:5173
>    ```
> 

### 6.3 テストアカウント

> ### 6.3 测试账号

| ユーザー名 | パスワード | 氏名 | ロール | 用途 |
|--------|------|------|------|------|
| admin | 123456 | 王经理 | MANAGER | マネージャー承認 + テンプレート管理 |
| zhangsan | 123456 | 张三 | EMPLOYEE | 休暇申請の送信 |
| zongjian1 | 123456 | 张总监 | MANAGER | ディレクター承認 |
| lisi | 123456 | 李四 | EMPLOYEE | 製品部社員 |
| zongjian2 | 123456 | 李总监 | MANAGER | 製品部ディレクター |

> 
> | 用户名 | 密码 | 姓名 | 角色 | 用途 |
> |--------|------|------|------|------|
> | admin | 123456 | 王经理 | MANAGER | 经理审批 + 模板管理 |
> | zhangsan | 123456 | 张三 | EMPLOYEE | 提交请假申请 |
> | zongjian1 | 123456 | 张总监 | MANAGER | 总监审批 |
> | lisi | 123456 | 李四 | EMPLOYEE | 产品部员工 |
> | zongjian2 | 123456 | 李总监 | MANAGER | 产品部总监 |
> 

**推奨テストフロー：**

> **推荐测试流程：**

1. `zhangsan` でログイン → 休暇申請を 1 件送信
2. `admin` でログイン → 自分の承認 → 承認待ち → 承認
3. `zhangsan` でログイン → 申請状態を確認

> 
> 1. 用 `zhangsan` 登录 → 提交一条请假申请
> 2. 用 `admin` 登录 → 我的审批 → 待审批 → 通过
> 3. 用 `zhangsan` 登录 → 查看申请状态
> 

---

## 七、プロジェクト構造

> ## 七、项目结构

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
├── docs/                         # SQL マイグレーションスクリプト + 技術文書
├── CLAUDE.md                     # プロジェクト説明と開発進捗
└── README.md                     # プロジェクト README
```

> 
> ```
> smartoa/
> ├── backend/
> │   ├── src/main/java/com/smartoa/
> │   │   ├── common/                   # Result<T>、BusinessException、GlobalExceptionHandler
> │   │   ├── config/                   # JWT 配置、CORS、过滤器
> │   │   ├── controller/               # 5 个 REST 控制器
> │   │   ├── dto/                      # 数据传输对象
> │   │   ├── entity/                   # 7 个实体类
> │   │   ├── mapper/                   # 7 个 MyBatis-Plus Mapper
> │   │   └── service/                  # 5 个 Service + TimeoutScheduler
> │   ├── src/main/resources/
> │   │   └── application.properties
> │   └── pom.xml
> ├── frontend/
> │   └── src/
> │       ├── api/                  # auth.js / leave.js / template.js
> │       ├── components/           # 6 个共享组件
> │       ├── layouts/              # AuthLayout / MainLayout
> │       ├── router/               # index.js
> │       ├── stores/               # auth.js / approval.js / users.js
> │       ├── utils/                # constants.js
> │       └── views/                # 15 个 Page 组件
> ├── docs/                         # SQL 迁移脚本 + 技术文档
> ├── CLAUDE.md                     # 项目说明与开发进度
> └── README.md                     # 项目 README
> ```
> 

---

-- SmartOA P2 技術文書 · 完 --

> — SmartOA P2 技术文档 · 完 —