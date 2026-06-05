Here is the complete translated markdown:

---

# データベース設計ドキュメント

> <span style="color: #888888;"># 数据库设计文档</span>

## SmartOA 承認フロー管理システム

> <span style="color: #888888;">## SmartOA 审批流管理系统</span>

---

## 1. データベース概要

> <span style="color: #888888;">## 1. 数据库概览</span>

| 項目 | 値 |
> <span style="color: #888888;">| 项 | 值 |</span>
|---|---|
| データベース名 | smartoa |
> <span style="color: #888888;">| 数据库名 | smartoa |</span>
| 文字セット | utf8mb4 |
> <span style="color: #888888;">| 字符集 | utf8mb4 |</span>
| 照合順序 | utf8mb4_unicode_ci |
> <span style="color: #888888;">| 排序规则 | utf8mb4_unicode_ci |</span>
| ストレージエンジン | InnoDB |
> <span style="color: #888888;">| 存储引擎 | InnoDB |</span>
| テーブル数 | 8 |
> <span style="color: #888888;">| 表数量 | 8 |</span>

---

## 2. ER 図（テキスト版）

> <span style="color: #888888;">## 2. ER 图（文字版）</span>

```
sys_user (ユーザー)
> <span style="color: #888888;">sys_user (用户)</span>
  │
  ├─(applicant_id)── leave_request (休暇申請)
> <span style="color: #888888;">  ├─(applicant_id)── leave_request (请假申请)</span>
  │                      │
  │                      ├─(template_id)── approval_template (承認テンプレート)
> <span style="color: #888888;">  │                      ├─(template_id)── approval_template (审批模板)</span>
  │                      │                      │
  │                      │                      ├─(1:N)── approval_node (承認ノード)
> <span style="color: #888888;">  │                      │                      ├─(1:N)── approval_node (审批节点)</span>
  │                      │                      └─(1:N)── template_field (テンプレートフィールド)
> <span style="color: #888888;">  │                      │                      └─(1:N)── template_field (模板字段)</span>
  │                      │
  │                      ├─(1:N)── approval_record (承認記録)
> <span style="color: #888888;">  │                      ├─(1:N)── approval_record (审批记录)</span>
  │                      │              │
  │                      │              └─(node_id)── approval_node
  │                      │
  │                      └─(1:N)── approval_task (並行承認タスク)
> <span style="color: #888888;">  │                      └─(1:N)── approval_task (并行审批任务)</span>
  │                                      │
  │                                      └─(node_id)── approval_node
  │
  └─(direct_leader_id / department_head_id)── sys_user (組織階層)
> <span style="color: #888888;">  └─(direct_leader_id / department_head_id)── sys_user (组织层级)</span>
```

---

## 3. テーブル構造設計

> <span style="color: #888888;">## 3. 表结构设计</span>

### 3.1 sys_user（ユーザーテーブル）

> <span style="color: #888888;">### 3.1 sys_user（用户表）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ユーザー ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 用户 ID |</span>
| username | VARCHAR(50) | NOT NULL, UNIQUE | ログインユーザー名 |
> <span style="color: #888888;">| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录用户名 |</span>
| password | VARCHAR(100) | NOT NULL | ログインパスワード（BCrypt 暗号化） |
> <span style="color: #888888;">| password | VARCHAR(100) | NOT NULL | 登录密码（BCrypt 加密） |</span>
| real_name | VARCHAR(50) | NOT NULL | 氏名 |
> <span style="color: #888888;">| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |</span>
| role | VARCHAR(20) | NOT NULL | ロール：EMPLOYEE / MANAGER |
> <span style="color: #888888;">| role | VARCHAR(20) | NOT NULL | 角色：EMPLOYEE / MANAGER |</span>
| department | VARCHAR(50) | | 所属部門 |
> <span style="color: #888888;">| department | VARCHAR(50) | | 所属部门 |</span>
| direct_leader_id | BIGINT | FK → sys_user.id | 直属上司 ID |
> <span style="color: #888888;">| direct_leader_id | BIGINT | FK → sys_user.id | 直属领导 ID |</span>
| department_head_id | BIGINT | FK → sys_user.id | 部門責任者 ID |
> <span style="color: #888888;">| department_head_id | BIGINT | FK → sys_user.id | 部门总监 ID |</span>

**シードデータ：**

> <span style="color: #888888;">**种子数据：**</span>

| id | username | real_name | role | department | direct_leader_id | department_head_id |
|---|---|---|---|---|---|---|
| 1 | admin | 王经理 | MANAGER | 技术部 | NULL | 4 |
| 2 | zhangsan | 张三 | EMPLOYEE | 技术部 | 1 | 4 |
| 3 | lisi | 李四 | EMPLOYEE | 产品部 | 1 | 5 |
| 4 | zongjian1 | 张总监 | MANAGER | 技术部 | NULL | NULL |
| 5 | zongjian2 | 李总监 | MANAGER | 产品部 | NULL | NULL |

**組織階層のイメージ：**
> <span style="color: #888888;">**组织层级示意：**</span>
```
张总监(4)                   李总监(5)
  └─ 王经理(1) ─┐             └─ 李四(3)
       ├─ 张三(2)
```

---

### 3.2 approval_template（承認テンプレートテーブル）

> <span style="color: #888888;">### 3.2 approval_template（审批模板表）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | テンプレート ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 模板 ID |</span>
| name | VARCHAR(100) | NOT NULL | テンプレート名 |
> <span style="color: #888888;">| name | VARCHAR(100) | NOT NULL | 模板名称 |</span>
| description | VARCHAR(500) | | テンプレート説明 |
> <span style="color: #888888;">| description | VARCHAR(500) | | 模板描述 |</span>
| enabled | BIT | NOT NULL, DEFAULT 1 | 有効かどうか |
> <span style="color: #888888;">| enabled | BIT | NOT NULL, DEFAULT 1 | 是否启用 |</span>
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
> <span style="color: #888888;">| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |</span>
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |
> <span style="color: #888888;">| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |</span>

---

### 3.3 approval_node（承認ノードテーブル）

> <span style="color: #888888;">### 3.3 approval_node（审批节点表）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ノード ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 节点 ID |</span>
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属テンプレート |
> <span style="color: #888888;">| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属模板 |</span>
| node_name | VARCHAR(100) | NOT NULL | ノード名 |
> <span style="color: #888888;">| node_name | VARCHAR(100) | NOT NULL | 节点名称 |</span>
| sort_order | INT | NOT NULL, DEFAULT 0 | 順序番号（0 から） |
> <span style="color: #888888;">| sort_order | INT | NOT NULL, DEFAULT 0 | 顺序号（0 起） |</span>
| approver_type | VARCHAR(30) | NOT NULL | 承認者タイプ |
> <span style="color: #888888;">| approver_type | VARCHAR(30) | NOT NULL | 审批人类型 |</span>
| approver_id | BIGINT | | 指定承認者 ID（SPECIFIC_USER + SINGLE） |
> <span style="color: #888888;">| approver_id | BIGINT | | 指定审批人 ID（SPECIFIC_USER + SINGLE） |</span>
| condition_expression | VARCHAR(500) | | SpEL 条件式（P2） |
> <span style="color: #888888;">| condition_expression | VARCHAR(500) | | SpEL 条件表达式（P2） |</span>
| sign_type | VARCHAR(20) | NOT NULL, DEFAULT 'SINGLE' | 署名モード（P2） |
> <span style="color: #888888;">| sign_type | VARCHAR(20) | NOT NULL, DEFAULT 'SINGLE' | 签批模式（P2） |</span>
| approver_ids | VARCHAR(1000) | | 並行承認者 ID リスト、カンマ区切り（P2） |
> <span style="color: #888888;">| approver_ids | VARCHAR(1000) | | 并行审批人 ID 列表，逗号分隔（P2） |</span>
| timeout_hours | INT | | タイムアウト時間数（P2） |
> <span style="color: #888888;">| timeout_hours | INT | | 超时小时数（P2） |</span>
| timeout_action | VARCHAR(30) | | タイムアウトアクション（P2） |
> <span style="color: #888888;">| timeout_action | VARCHAR(30) | | 超时动作（P2） |</span>
| escalate_to_user_id | BIGINT | | タイムアウト転送先ユーザー ID（P2） |
> <span style="color: #888888;">| escalate_to_user_id | BIGINT | | 超时转派目标用户 ID（P2） |</span>
| create_time | DATETIME | NOT NULL | 作成日時 |
> <span style="color: #888888;">| create_time | DATETIME | NOT NULL | 创建时间 |</span>
| update_time | DATETIME | NOT NULL | 更新日時 |
> <span style="color: #888888;">| update_time | DATETIME | NOT NULL | 更新时间 |</span>

**一意制約：** `(template_id, sort_order)`

> <span style="color: #888888;">**唯一约束：** `(template_id, sort_order)`</span>

**承認者タイプ列挙：**

> <span style="color: #888888;">**审批人类型枚举：**</span>

| 値 | 説明 |
> <span style="color: #888888;">| 值 | 说明 |</span>
|---|---|
| DIRECT_LEADER | 申請者の直属上司 |
> <span style="color: #888888;">| DIRECT_LEADER | 申请人的直属领导 |</span>
| DEPARTMENT_HEAD | 申請者の所属部門の責任者 |
> <span style="color: #888888;">| DEPARTMENT_HEAD | 申请人所属部门的总监 |</span>
| SPECIFIC_USER | 指定ユーザー |
> <span style="color: #888888;">| SPECIFIC_USER | 指定用户 |</span>

**署名モード列挙（P2）：**

> <span style="color: #888888;">**签批模式枚举（P2）：**</span>

| 値 | 説明 |
> <span style="color: #888888;">| 值 | 说明 |</span>
|---|---|
| SINGLE | 単独承認（デフォルト） |
> <span style="color: #888888;">| SINGLE | 单人审批（默认） |</span>
| COUNTER_SIGN | 会签 — 全承認者が同意して初めて進行 |
> <span style="color: #888888;">| COUNTER_SIGN | 会签 — 全部审批人同意才推进 |</span>
| OR_SIGN | 或签 — いずれかの承認者が同意すれば進行 |
> <span style="color: #888888;">| OR_SIGN | 或签 — 任一审批人同意即推进 |</span>

**タイムアウトアクション列挙（P2）：**

> <span style="color: #888888;">**超时动作枚举（P2）：**</span>

| 値 | 説明 |
> <span style="color: #888888;">| 值 | 说明 |</span>
|---|---|
| ESCALATE | escalate_to_user_id へ転送、またはノードをスキップ |
> <span style="color: #888888;">| ESCALATE | 转派给 escalate_to_user_id 或跳过节点 |</span>
| AUTO_APPROVE | 現在のノードを自動承認 |
> <span style="color: #888888;">| AUTO_APPROVE | 自动通过当前节点 |</span>
| AUTO_REJECT | 自動却下、フロー終了 |
> <span style="color: #888888;">| AUTO_REJECT | 自动驳回，终止流程 |</span>

---

### 3.4 template_field（テンプレートフォームフィールドテーブル）

> <span style="color: #888888;">### 3.4 template_field（模板表单字段表）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | フィールド ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 字段 ID |</span>
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属テンプレート |
> <span style="color: #888888;">| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属模板 |</span>
| field_name | VARCHAR(50) | NOT NULL | フィールド名（英語） |
> <span style="color: #888888;">| field_name | VARCHAR(50) | NOT NULL | 字段名（英文） |</span>
| field_label | VARCHAR(100) | NOT NULL | 表示名（中国語） |
> <span style="color: #888888;">| field_label | VARCHAR(100) | NOT NULL | 显示名（中文） |</span>
| field_type | VARCHAR(30) | NOT NULL | フィールド型 |
> <span style="color: #888888;">| field_type | VARCHAR(30) | NOT NULL | 字段类型 |</span>
| required | BIT | NOT NULL, DEFAULT 1 | 必須かどうか |
> <span style="color: #888888;">| required | BIT | NOT NULL, DEFAULT 1 | 是否必填 |</span>
| sort_order | INT | NOT NULL, DEFAULT 0 | ソート番号 |
> <span style="color: #888888;">| sort_order | INT | NOT NULL, DEFAULT 0 | 排序号 |</span>
| options | VARCHAR(500) | | SELECT 型の JSON オプション |
> <span style="color: #888888;">| options | VARCHAR(500) | | SELECT 类型的 JSON 选项 |</span>
| create_time | DATETIME | NOT NULL | 作成日時 |
> <span style="color: #888888;">| create_time | DATETIME | NOT NULL | 创建时间 |</span>

**一意制約：** `(template_id, field_name)`

> <span style="color: #888888;">**唯一约束：** `(template_id, field_name)`</span>

**フィールド型列挙：** TEXT / DATE / SELECT / TEXTAREA / NUMBER

> <span style="color: #888888;">**字段类型枚举：** TEXT / DATE / SELECT / TEXTAREA / NUMBER</span>

---

### 3.5 leave_request（休暇申請テーブル）

> <span style="color: #888888;">### 3.5 leave_request（请假申请表）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 申請 ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 申请 ID |</span>
| applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申請者 ID |
> <span style="color: #888888;">| applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申请人 ID |</span>
| template_id | BIGINT | FK → approval_template.id | 関連する承認テンプレート |
> <span style="color: #888888;">| template_id | BIGINT | FK → approval_template.id | 关联的审批模板 |</span>
| leave_type | VARCHAR(20) | NOT NULL | 休暇種別 |
> <span style="color: #888888;">| leave_type | VARCHAR(20) | NOT NULL | 请假类型 |</span>
| start_date | DATE | NOT NULL | 開始日 |
> <span style="color: #888888;">| start_date | DATE | NOT NULL | 开始日期 |</span>
| end_date | DATE | NOT NULL | 終了日 |
> <span style="color: #888888;">| end_date | DATE | NOT NULL | 结束日期 |</span>
| reason | VARCHAR(500) | | 休暇理由 |
> <span style="color: #888888;">| reason | VARCHAR(500) | | 请假原因 |</span>
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | ステータス |
> <span style="color: #888888;">| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 状态 |</span>
| current_node_id | BIGINT | FK → approval_node.id | 現在の承認ノード ID |
> <span style="color: #888888;">| current_node_id | BIGINT | FK → approval_node.id | 当前审批节点 ID |</span>
| current_approver_id | BIGINT | FK → sys_user.id | 現在の承認者 ID（SINGLE モード） |
> <span style="color: #888888;">| current_approver_id | BIGINT | FK → sys_user.id | 当前审批人 ID（SINGLE 模式） |</span>
| timeout_time | DATETIME | | タイムアウト期限（P2） |
> <span style="color: #888888;">| timeout_time | DATETIME | | 超时截止时间（P2） |</span>
| create_time | DATETIME | NOT NULL | 提出日時 |
> <span style="color: #888888;">| create_time | DATETIME | NOT NULL | 提交时间 |</span>
| update_time | DATETIME | NOT NULL | 更新日時 |
> <span style="color: #888888;">| update_time | DATETIME | NOT NULL | 更新时间 |</span>

**ステータス列挙：**

> <span style="color: #888888;">**状态枚举：**</span>

| 値 | 説明 |
> <span style="color: #888888;">| 值 | 说明 |</span>
|---|---|
| PENDING | 承認中 |
> <span style="color: #888888;">| PENDING | 审批中 |</span>
| APPROVED | 承認済み |
> <span style="color: #888888;">| APPROVED | 已通过 |</span>
| REJECTED | 却下済み |
> <span style="color: #888888;">| REJECTED | 已驳回 |</span>
| WITHDRAWN | 取下げ済み |
> <span style="color: #888888;">| WITHDRAWN | 已撤回 |</span>

---

### 3.6 approval_record（承認記録テーブル）

> <span style="color: #888888;">### 3.6 approval_record（审批记录表）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 記録 ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 记录 ID |</span>
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
> <span style="color: #888888;">| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 关联申请 ID |</span>
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID（0=システム自動操作） |
> <span style="color: #888888;">| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 审批人 ID（0=系统自动操作） |</span>
| action | VARCHAR(20) | NOT NULL | 操作種別 |
> <span style="color: #888888;">| action | VARCHAR(20) | NOT NULL | 操作类型 |</span>
| comment | VARCHAR(500) | | 承認コメント |
> <span style="color: #888888;">| comment | VARCHAR(500) | | 审批意见 |</span>
| node_id | BIGINT | FK → approval_node.id | 操作時のノード ID |
> <span style="color: #888888;">| node_id | BIGINT | FK → approval_node.id | 操作时的节点 ID |</span>
| create_time | DATETIME | NOT NULL | 操作日時 |
> <span style="color: #888888;">| create_time | DATETIME | NOT NULL | 操作时间 |</span>

**操作種別列挙：**

> <span style="color: #888888;">**操作类型枚举：**</span>

| 値 | 説明 |
> <span style="color: #888888;">| 值 | 说明 |</span>
|---|---|
| APPROVE | 承認 |
> <span style="color: #888888;">| APPROVE | 通过 |</span>
| REJECT | 却下 |
> <span style="color: #888888;">| REJECT | 驳回 |</span>
| WITHDRAW | 取下げ |
> <span style="color: #888888;">| WITHDRAW | 撤回 |</span>
| TRANSFER | 転送 |
> <span style="color: #888888;">| TRANSFER | 转派 |</span>
| TIMEOUT_ESCALATE | タイムアウト転送 |
> <span style="color: #888888;">| TIMEOUT_ESCALATE | 超时转派 |</span>
| TIMEOUT_APPROVE | タイムアウト自動承認 |
> <span style="color: #888888;">| TIMEOUT_APPROVE | 超时自动通过 |</span>
| TIMEOUT_REJECT | タイムアウト自動却下 |
> <span style="color: #888888;">| TIMEOUT_REJECT | 超时自动驳回 |</span>

---

### 3.7 approval_task（並行承認タスクテーブル — P2 新規追加）

> <span style="color: #888888;">### 3.7 approval_task（并行审批任务表 — P2 新增）</span>

| フィールド | 型 | 制約 | 説明 |
> <span style="color: #888888;">| 字段 | 类型 | 约束 | 说明 |</span>
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | タスク ID |
> <span style="color: #888888;">| id | BIGINT | PK, AUTO_INCREMENT | 任务 ID |</span>
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
> <span style="color: #888888;">| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 关联申请 ID |</span>
| node_id | BIGINT | NOT NULL, FK → approval_node.id | 関連ノード ID |
> <span style="color: #888888;">| node_id | BIGINT | NOT NULL, FK → approval_node.id | 关联节点 ID |</span>
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID |
> <span style="color: #888888;">| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 审批人 ID |</span>
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | タスクステータス |
> <span style="color: #888888;">| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 任务状态 |</span>
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
> <span style="color: #888888;">| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |</span>
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |
> <span style="color: #888888;">| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |</span>

**一意制約：** `(leave_request_id, node_id, approver_id)`

> <span style="color: #888888;">**唯一约束：** `(leave_request_id, node_id, approver_id)`</span>

**タスクステータス列挙：**

> <span style="color: #888888;">**任务状态枚举：**</span>

| 値 | 説明 |
> <span style="color: #888888;">| 值 | 说明 |</span>
|---|---|
| PENDING | 承認待ち |
> <span style="color: #888888;">| PENDING | 待审批 |</span>
| COMPLETED | 承認済み |
> <span style="color: #888888;">| COMPLETED | 已审批 |</span>
| SKIPPED | スキップ済み（他者の操作による） |
> <span style="color: #888888;">| SKIPPED | 已跳过（他人操作导致） |</span>

---

## 4. インデックス設計

> <span style="color: #888888;">## 4. 索引设计</span>

| テーブル | インデックス種別 | フィールド |
> <span style="color: #888888;">| 表 | 索引类型 | 字段 |</span>
|---|---|---|
| sys_user | UNIQUE | username |
| approval_node | UNIQUE | (template_id, sort_order) |
| approval_node | FOREIGN KEY | template_id |
| template_field | UNIQUE | (template_id, field_name) |
| template_field | FOREIGN KEY | template_id |
| leave_request | FOREIGN KEY | applicant_id |
| leave_request | FOREIGN KEY | template_id |
| leave_request | FOREIGN KEY | current_node_id |
| leave_request | INDEX | (status, timeout_time) |
| approval_record | FOREIGN KEY | leave_request_id |
| approval_record | FOREIGN KEY | approver_id |
| approval_record | FOREIGN KEY | node_id |
| approval_task | UNIQUE | (leave_request_id, node_id, approver_id) |
| approval_task | FOREIGN KEY | leave_request_id |
| approval_task | FOREIGN KEY | node_id |

---

## 5. マイグレーションスクリプト

> <span style="color: #888888;">## 5. 迁移脚本</span>

| スクリプト | 説明 |
> <span style="color: #888888;">| 脚本 | 说明 |</span>
|---|---|
| `docs/mysql-p0-upgrade.sql` | P0 初期化：データベース作成 + 4 テーブル + シードデータ |
> <span style="color: #888888;">| `docs/mysql-p0-upgrade.sql` | P0 初始化：建库 + 4 表 + 种子数据 |</span>
| `docs/mysql-p1-upgrade.sql` | P1 アップグレード：approval_node + template_field + 新フィールド |
> <span style="color: #888888;">| `docs/mysql-p1-upgrade.sql` | P1 升级：approval_node + template_field + 新字段 |</span>
| `docs/mysql-p3-bcrypt.sql` | BCrypt パスワード移行 |
> <span style="color: #888888;">| `docs/mysql-p3-bcrypt.sql` | BCrypt 密码迁移 |</span>
| `docs/mysql-p4-parallel.sql` | P2 並行承認：sign_type + approver_ids + approval_task テーブル |
> <span style="color: #888888;">| `docs/mysql-p4-parallel.sql` | P2 并行审批：sign_type + approver_ids + approval_task 表 |</span>
| `docs/mysql-p5-timeout.sql` | P2 タイムアウトアップグレード：timeout_hours + timeout_action + escalate_to_user_id + timeout_time |
> <span style="color: #888888;">| `docs/mysql-p5-timeout.sql` | P2 超时升级：timeout_hours + timeout_action + escalate_to_user_id + timeout_time |</span>

---

> ドキュメントバージョン：v2.0（P2 完了） | 更新日：2026-05-27
> <span style="color: #888888;">> 文档版本：v2.0（P2 完成） | 更新日期：2026-05-27</span>