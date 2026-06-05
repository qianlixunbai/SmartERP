Here is the complete translated markdown:

---

# データベース設計ドキュメント

> # 数据库设计文档

## SmartOA 承認フロー管理システム

> ## SmartOA 审批流管理系统

---

## 1. データベース概要

> ## 1. 数据库概览

| 項目 | 値 |
> | 项 | 值 |
|---|---|
| データベース名 | smartoa |
> | 数据库名 | smartoa |
| 文字セット | utf8mb4 |
> | 字符集 | utf8mb4 |
| 照合順序 | utf8mb4_unicode_ci |
> | 排序规则 | utf8mb4_unicode_ci |
| ストレージエンジン | InnoDB |
> | 存储引擎 | InnoDB |
| テーブル数 | 8 |
> | 表数量 | 8 |

---

## 2. ER 図（テキスト版）

> ## 2. ER 图（文字版）

```
sys_user (ユーザー)
> sys_user (用户)
  │
  ├─(applicant_id)── leave_request (休暇申請)
>   ├─(applicant_id)── leave_request (请假申请)
  │                      │
  │                      ├─(template_id)── approval_template (承認テンプレート)
>   │                      ├─(template_id)── approval_template (审批模板)
  │                      │                      │
  │                      │                      ├─(1:N)── approval_node (承認ノード)
>   │                      │                      ├─(1:N)── approval_node (审批节点)
  │                      │                      └─(1:N)── template_field (テンプレートフィールド)
>   │                      │                      └─(1:N)── template_field (模板字段)
  │                      │
  │                      ├─(1:N)── approval_record (承認記録)
>   │                      ├─(1:N)── approval_record (审批记录)
  │                      │              │
  │                      │              └─(node_id)── approval_node
  │                      │
  │                      └─(1:N)── approval_task (並行承認タスク)
>   │                      └─(1:N)── approval_task (并行审批任务)
  │                                      │
  │                                      └─(node_id)── approval_node
  │
  └─(direct_leader_id / department_head_id)── sys_user (組織階層)
>   └─(direct_leader_id / department_head_id)── sys_user (组织层级)
```

---

## 3. テーブル構造設計

> ## 3. 表结构设计

### 3.1 sys_user（ユーザーテーブル）

> ### 3.1 sys_user（用户表）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ユーザー ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | ログインユーザー名 |
> | username | VARCHAR(50) | NOT NULL, UNIQUE | 登录用户名 |
| password | VARCHAR(100) | NOT NULL | ログインパスワード（BCrypt 暗号化） |
> | password | VARCHAR(100) | NOT NULL | 登录密码（BCrypt 加密） |
| real_name | VARCHAR(50) | NOT NULL | 氏名 |
> | real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | ロール：EMPLOYEE / MANAGER |
> | role | VARCHAR(20) | NOT NULL | 角色：EMPLOYEE / MANAGER |
| department | VARCHAR(50) | | 所属部門 |
> | department | VARCHAR(50) | | 所属部门 |
| direct_leader_id | BIGINT | FK → sys_user.id | 直属上司 ID |
> | direct_leader_id | BIGINT | FK → sys_user.id | 直属领导 ID |
| department_head_id | BIGINT | FK → sys_user.id | 部門責任者 ID |
> | department_head_id | BIGINT | FK → sys_user.id | 部门总监 ID |

**シードデータ：**

> **种子数据：**

| id | username | real_name | role | department | direct_leader_id | department_head_id |
|---|---|---|---|---|---|---|
| 1 | admin | 王经理 | MANAGER | 技术部 | NULL | 4 |
| 2 | zhangsan | 张三 | EMPLOYEE | 技术部 | 1 | 4 |
| 3 | lisi | 李四 | EMPLOYEE | 产品部 | 1 | 5 |
| 4 | zongjian1 | 张总监 | MANAGER | 技术部 | NULL | NULL |
| 5 | zongjian2 | 李总监 | MANAGER | 产品部 | NULL | NULL |

**組織階層のイメージ：**
> **组织层级示意：**
```
张总监(4)                   李总监(5)
  └─ 王经理(1) ─┐             └─ 李四(3)
       ├─ 张三(2)
```

---

### 3.2 approval_template（承認テンプレートテーブル）

> ### 3.2 approval_template（审批模板表）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | テンプレート ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 模板 ID |
| name | VARCHAR(100) | NOT NULL | テンプレート名 |
> | name | VARCHAR(100) | NOT NULL | 模板名称 |
| description | VARCHAR(500) | | テンプレート説明 |
> | description | VARCHAR(500) | | 模板描述 |
| enabled | BIT | NOT NULL, DEFAULT 1 | 有効かどうか |
> | enabled | BIT | NOT NULL, DEFAULT 1 | 是否启用 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
> | create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |
> | update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

---

### 3.3 approval_node（承認ノードテーブル）

> ### 3.3 approval_node（审批节点表）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ノード ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 节点 ID |
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属テンプレート |
> | template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属模板 |
| node_name | VARCHAR(100) | NOT NULL | ノード名 |
> | node_name | VARCHAR(100) | NOT NULL | 节点名称 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 順序番号（0 から） |
> | sort_order | INT | NOT NULL, DEFAULT 0 | 顺序号（0 起） |
| approver_type | VARCHAR(30) | NOT NULL | 承認者タイプ |
> | approver_type | VARCHAR(30) | NOT NULL | 审批人类型 |
| approver_id | BIGINT | | 指定承認者 ID（SPECIFIC_USER + SINGLE） |
> | approver_id | BIGINT | | 指定审批人 ID（SPECIFIC_USER + SINGLE） |
| condition_expression | VARCHAR(500) | | SpEL 条件式（P2） |
> | condition_expression | VARCHAR(500) | | SpEL 条件表达式（P2） |
| sign_type | VARCHAR(20) | NOT NULL, DEFAULT 'SINGLE' | 署名モード（P2） |
> | sign_type | VARCHAR(20) | NOT NULL, DEFAULT 'SINGLE' | 签批模式（P2） |
| approver_ids | VARCHAR(1000) | | 並行承認者 ID リスト、カンマ区切り（P2） |
> | approver_ids | VARCHAR(1000) | | 并行审批人 ID 列表，逗号分隔（P2） |
| timeout_hours | INT | | タイムアウト時間数（P2） |
> | timeout_hours | INT | | 超时小时数（P2） |
| timeout_action | VARCHAR(30) | | タイムアウトアクション（P2） |
> | timeout_action | VARCHAR(30) | | 超时动作（P2） |
| escalate_to_user_id | BIGINT | | タイムアウト転送先ユーザー ID（P2） |
> | escalate_to_user_id | BIGINT | | 超时转派目标用户 ID（P2） |
| create_time | DATETIME | NOT NULL | 作成日時 |
> | create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新日時 |
> | update_time | DATETIME | NOT NULL | 更新时间 |

**一意制約：** `(template_id, sort_order)`

> **唯一约束：** `(template_id, sort_order)`

**承認者タイプ列挙：**

> **审批人类型枚举：**

| 値 | 説明 |
> | 值 | 说明 |
|---|---|
| DIRECT_LEADER | 申請者の直属上司 |
> | DIRECT_LEADER | 申请人的直属领导 |
| DEPARTMENT_HEAD | 申請者の所属部門の責任者 |
> | DEPARTMENT_HEAD | 申请人所属部门的总监 |
| SPECIFIC_USER | 指定ユーザー |
> | SPECIFIC_USER | 指定用户 |

**署名モード列挙（P2）：**

> **签批模式枚举（P2）：**

| 値 | 説明 |
> | 值 | 说明 |
|---|---|
| SINGLE | 単独承認（デフォルト） |
> | SINGLE | 单人审批（默认） |
| COUNTER_SIGN | 会签 — 全承認者が同意して初めて進行 |
> | COUNTER_SIGN | 会签 — 全部审批人同意才推进 |
| OR_SIGN | 或签 — いずれかの承認者が同意すれば進行 |
> | OR_SIGN | 或签 — 任一审批人同意即推进 |

**タイムアウトアクション列挙（P2）：**

> **超时动作枚举（P2）：**

| 値 | 説明 |
> | 值 | 说明 |
|---|---|
| ESCALATE | escalate_to_user_id へ転送、またはノードをスキップ |
> | ESCALATE | 转派给 escalate_to_user_id 或跳过节点 |
| AUTO_APPROVE | 現在のノードを自動承認 |
> | AUTO_APPROVE | 自动通过当前节点 |
| AUTO_REJECT | 自動却下、フロー終了 |
> | AUTO_REJECT | 自动驳回，终止流程 |

---

### 3.4 template_field（テンプレートフォームフィールドテーブル）

> ### 3.4 template_field（模板表单字段表）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | フィールド ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 字段 ID |
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属テンプレート |
> | template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属模板 |
| field_name | VARCHAR(50) | NOT NULL | フィールド名（英語） |
> | field_name | VARCHAR(50) | NOT NULL | 字段名（英文） |
| field_label | VARCHAR(100) | NOT NULL | 表示名（中国語） |
> | field_label | VARCHAR(100) | NOT NULL | 显示名（中文） |
| field_type | VARCHAR(30) | NOT NULL | フィールド型 |
> | field_type | VARCHAR(30) | NOT NULL | 字段类型 |
| required | BIT | NOT NULL, DEFAULT 1 | 必須かどうか |
> | required | BIT | NOT NULL, DEFAULT 1 | 是否必填 |
| sort_order | INT | NOT NULL, DEFAULT 0 | ソート番号 |
> | sort_order | INT | NOT NULL, DEFAULT 0 | 排序号 |
| options | VARCHAR(500) | | SELECT 型の JSON オプション |
> | options | VARCHAR(500) | | SELECT 类型的 JSON 选项 |
| create_time | DATETIME | NOT NULL | 作成日時 |
> | create_time | DATETIME | NOT NULL | 创建时间 |

**一意制約：** `(template_id, field_name)`

> **唯一约束：** `(template_id, field_name)`

**フィールド型列挙：** TEXT / DATE / SELECT / TEXTAREA / NUMBER

> **字段类型枚举：** TEXT / DATE / SELECT / TEXTAREA / NUMBER

---

### 3.5 leave_request（休暇申請テーブル）

> ### 3.5 leave_request（请假申请表）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 申請 ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 申请 ID |
| applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申請者 ID |
> | applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申请人 ID |
| template_id | BIGINT | FK → approval_template.id | 関連する承認テンプレート |
> | template_id | BIGINT | FK → approval_template.id | 关联的审批模板 |
| leave_type | VARCHAR(20) | NOT NULL | 休暇種別 |
> | leave_type | VARCHAR(20) | NOT NULL | 请假类型 |
| start_date | DATE | NOT NULL | 開始日 |
> | start_date | DATE | NOT NULL | 开始日期 |
| end_date | DATE | NOT NULL | 終了日 |
> | end_date | DATE | NOT NULL | 结束日期 |
| reason | VARCHAR(500) | | 休暇理由 |
> | reason | VARCHAR(500) | | 请假原因 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | ステータス |
> | status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 状态 |
| current_node_id | BIGINT | FK → approval_node.id | 現在の承認ノード ID |
> | current_node_id | BIGINT | FK → approval_node.id | 当前审批节点 ID |
| current_approver_id | BIGINT | FK → sys_user.id | 現在の承認者 ID（SINGLE モード） |
> | current_approver_id | BIGINT | FK → sys_user.id | 当前审批人 ID（SINGLE 模式） |
| timeout_time | DATETIME | | タイムアウト期限（P2） |
> | timeout_time | DATETIME | | 超时截止时间（P2） |
| create_time | DATETIME | NOT NULL | 提出日時 |
> | create_time | DATETIME | NOT NULL | 提交时间 |
| update_time | DATETIME | NOT NULL | 更新日時 |
> | update_time | DATETIME | NOT NULL | 更新时间 |

**ステータス列挙：**

> **状态枚举：**

| 値 | 説明 |
> | 值 | 说明 |
|---|---|
| PENDING | 承認中 |
> | PENDING | 审批中 |
| APPROVED | 承認済み |
> | APPROVED | 已通过 |
| REJECTED | 却下済み |
> | REJECTED | 已驳回 |
| WITHDRAWN | 取下げ済み |
> | WITHDRAWN | 已撤回 |

---

### 3.6 approval_record（承認記録テーブル）

> ### 3.6 approval_record（审批记录表）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 記録 ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 记录 ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
> | leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 关联申请 ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID（0=システム自動操作） |
> | approver_id | BIGINT | NOT NULL, FK → sys_user.id | 审批人 ID（0=系统自动操作） |
| action | VARCHAR(20) | NOT NULL | 操作種別 |
> | action | VARCHAR(20) | NOT NULL | 操作类型 |
| comment | VARCHAR(500) | | 承認コメント |
> | comment | VARCHAR(500) | | 审批意见 |
| node_id | BIGINT | FK → approval_node.id | 操作時のノード ID |
> | node_id | BIGINT | FK → approval_node.id | 操作时的节点 ID |
| create_time | DATETIME | NOT NULL | 操作日時 |
> | create_time | DATETIME | NOT NULL | 操作时间 |

**操作種別列挙：**

> **操作类型枚举：**

| 値 | 説明 |
> | 值 | 说明 |
|---|---|
| APPROVE | 承認 |
> | APPROVE | 通过 |
| REJECT | 却下 |
> | REJECT | 驳回 |
| WITHDRAW | 取下げ |
> | WITHDRAW | 撤回 |
| TRANSFER | 転送 |
> | TRANSFER | 转派 |
| TIMEOUT_ESCALATE | タイムアウト転送 |
> | TIMEOUT_ESCALATE | 超时转派 |
| TIMEOUT_APPROVE | タイムアウト自動承認 |
> | TIMEOUT_APPROVE | 超时自动通过 |
| TIMEOUT_REJECT | タイムアウト自動却下 |
> | TIMEOUT_REJECT | 超时自动驳回 |

---

### 3.7 approval_task（並行承認タスクテーブル — P2 新規追加）

> ### 3.7 approval_task（并行审批任务表 — P2 新增）

| フィールド | 型 | 制約 | 説明 |
> | 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | タスク ID |
> | id | BIGINT | PK, AUTO_INCREMENT | 任务 ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
> | leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 关联申请 ID |
| node_id | BIGINT | NOT NULL, FK → approval_node.id | 関連ノード ID |
> | node_id | BIGINT | NOT NULL, FK → approval_node.id | 关联节点 ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID |
> | approver_id | BIGINT | NOT NULL, FK → sys_user.id | 审批人 ID |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | タスクステータス |
> | status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 任务状态 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
> | create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |
> | update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**一意制約：** `(leave_request_id, node_id, approver_id)`

> **唯一约束：** `(leave_request_id, node_id, approver_id)`

**タスクステータス列挙：**

> **任务状态枚举：**

| 値 | 説明 |
> | 值 | 说明 |
|---|---|
| PENDING | 承認待ち |
> | PENDING | 待审批 |
| COMPLETED | 承認済み |
> | COMPLETED | 已审批 |
| SKIPPED | スキップ済み（他者の操作による） |
> | SKIPPED | 已跳过（他人操作导致） |

---

## 4. インデックス設計

> ## 4. 索引设计

| テーブル | インデックス種別 | フィールド |
> | 表 | 索引类型 | 字段 |
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

> ## 5. 迁移脚本

| スクリプト | 説明 |
> | 脚本 | 说明 |
|---|---|
| `docs/mysql-p0-upgrade.sql` | P0 初期化：データベース作成 + 4 テーブル + シードデータ |
> | `docs/mysql-p0-upgrade.sql` | P0 初始化：建库 + 4 表 + 种子数据 |
| `docs/mysql-p1-upgrade.sql` | P1 アップグレード：approval_node + template_field + 新フィールド |
> | `docs/mysql-p1-upgrade.sql` | P1 升级：approval_node + template_field + 新字段 |
| `docs/mysql-p3-bcrypt.sql` | BCrypt パスワード移行 |
> | `docs/mysql-p3-bcrypt.sql` | BCrypt 密码迁移 |
| `docs/mysql-p4-parallel.sql` | P2 並行承認：sign_type + approver_ids + approval_task テーブル |
> | `docs/mysql-p4-parallel.sql` | P2 并行审批：sign_type + approver_ids + approval_task 表 |
| `docs/mysql-p5-timeout.sql` | P2 タイムアウトアップグレード：timeout_hours + timeout_action + escalate_to_user_id + timeout_time |
> | `docs/mysql-p5-timeout.sql` | P2 超时升级：timeout_hours + timeout_action + escalate_to_user_id + timeout_time |

---

> ドキュメントバージョン：v2.0（P2 完了） | 更新日：2026-05-27
> > 文档版本：v2.0（P2 完成） | 更新日期：2026-05-27