# 数据库设计文档

## SmartOA 审批流管理系统

---

| 項目 | 値 |
|---|---|
| 照合順序 | utf8mb4_unicode_ci |

## 1. 数据库概览
| 项 | 值 |
|---|---|
| 数据库名 | smartoa |
| 字符集 | utf8mb4 |
| 排序规则 | utf8mb4_unicode_ci |
| 存储引擎 | InnoDB |
| 表数量 | 12 |

---

```
  │
  ├─(applicant_id)── leave_request (休暇申請)
  │                      │
  │                      │                      │
  │                      │
  │                      ├─(1:N)── approval_record (承認記録)
  │                      │              │
  │                      │              └─(node_id)── approval_node
  │                      │
  │                                      │
  │                                      └─(node_id)── approval_node
  │
  └─(direct_leader_id / department_head_id)── sys_user (組織階層)
```

## 2. ER 图（文字版）
```
sys_user (用户)
  │
  ├─(applicant_id)── leave_request (请假申请)
  │                      │
  │                      ├─(template_id)── approval_template (审批模板)
  │                      │                      │
  │                      │                      ├─(1:N)── approval_node (审批节点)
  │                      │                      └─(1:N)── template_field (模板字段)
  │                      │
  │                      ├─(1:N)── approval_record (审批记录)
  │                      │              │
  │                      │              └─(node_id)── approval_node
  │                      │
  │                      └─(1:N)── approval_task (并行审批任务)
  │                                      │
  │                                      └─(node_id)── approval_node
  │
  └─(direct_leader_id / department_head_id)── sys_user (组织层级)
```

---

|---|---|---|---|
| real_name | VARCHAR(50) | NOT NULL | 氏名 |
| department | VARCHAR(50) | | 所属部門 |
| direct_leader_id | BIGINT | FK → sys_user.id | 直属上司 ID |
| department_head_id | BIGINT | FK → sys_user.id | 部門責任者 ID |

### 3.1 sys_user（用户表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录用户名 |
| password | VARCHAR(100) | NOT NULL | 登录密码（BCrypt 加密） |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | 角色：EMPLOYEE / MANAGER |
| department | VARCHAR(50) | | 所属部门 |
| direct_leader_id | BIGINT | FK → sys_user.id | 直属领导 ID |
| department_head_id | BIGINT | FK → sys_user.id | 部门总监 ID |

| id | username | real_name | role | department | direct_leader_id | department_head_id |
|---|---|---|---|---|---|---|
| 1 | admin | 王经理 | MANAGER | 技術部 | NULL | 4 |
| 2 | zhangsan | 張三 | EMPLOYEE | 技術部 | 1 | 4 |
| 3 | lisi | 李四 | EMPLOYEE | 製品部 | 1 | 5 |
| 4 | zongjian1 | 張総監 | MANAGER | 技術部 | NULL | NULL |
| 5 | zongjian2 | 李総監 | MANAGER | 製品部 | NULL | NULL |

**种子数据：**
| id | username | real_name | role | department | direct_leader_id | department_head_id |
|---|---|---|---|---|---|---|
| 1 | admin | 王经理 | MANAGER | 技术部 | NULL | 4 |
| 2 | zhangsan | 张三 | EMPLOYEE | 技术部 | 1 | 4 |
| 3 | lisi | 李四 | EMPLOYEE | 产品部 | 1 | 5 |
| 4 | zongjian1 | 张总监 | MANAGER | 技术部 | NULL | NULL |
| 5 | zongjian2 | 李总监 | MANAGER | 产品部 | NULL | NULL |

```
張総監(4)                   李総監(5)
  └─ 王经理(1) ─┐             └─ 李四(3)
       ├─ 張三(2)
```

**组织层级示意：**
```
张总监(4)                   李总监(5)
  └─ 王经理(1) ─┐             └─ 李四(3)
       ├─ 张三(2)
```

---

|---|---|---|---|
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |

### 3.2 approval_template（审批模板表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 模板 ID |
| name | VARCHAR(100) | NOT NULL | 模板名称 |
| description | VARCHAR(500) | | 模板描述 |
| enabled | BIT | NOT NULL, DEFAULT 1 | 是否启用 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

---

|---|---|---|---|
| sort_order | INT | NOT NULL, DEFAULT 0 | 順序番号（0 起点） |
| approver_id | BIGINT | | 指定承認者 ID（SPECIFIC_USER + SINGLE） |
| condition_expression | VARCHAR(500) | | SpEL 条件式（P2） |
| create_time | DATETIME | NOT NULL | 作成日時 |
| update_time | DATETIME | NOT NULL | 更新日時 |

### 3.3 approval_node（审批节点表）⭐ 核心
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 节点 ID |
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属模板 |
| node_name | VARCHAR(100) | NOT NULL | 节点名称 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 顺序号（0 起） |
| approver_type | VARCHAR(30) | NOT NULL | 审批人类型 |
| approver_id | BIGINT | | 指定审批人 ID（SPECIFIC_USER + SINGLE） |
| condition_expression | VARCHAR(500) | | SpEL 条件表达式（P2） |
| sign_type | VARCHAR(20) | NOT NULL, DEFAULT 'SINGLE' | 签批模式（P2） |
| approver_ids | VARCHAR(1000) | | 并行审批人 ID 列表，逗号分隔（P2） |
| timeout_hours | INT | | 超时小时数（P2） |
| timeout_action | VARCHAR(30) | | 超时动作（P2） |
| escalate_to_user_id | BIGINT | | 超时转派目标用户 ID（P2） |
| create_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

**唯一制約：** `(template_id, sort_order)`

**唯一约束：** `(template_id, sort_order)`

| 値 | 説明 |
|---|---|

**审批人类型枚举：**
| 值 | 说明 |
|---|---|
| DIRECT_LEADER | 申请人的直属领导 |
| DEPARTMENT_HEAD | 申请人所属部门的总监 |
| SPECIFIC_USER | 指定用户 |

| 値 | 説明 |
|---|---|

**签批模式枚举（P2）：**
| 值 | 说明 |
|---|---|
| SINGLE | 单人审批（默认） |
| COUNTER_SIGN | 会签 — 全部审批人同意才推进 |
| OR_SIGN | 或签 — 任一审批人同意即推进 |

| 値 | 説明 |
|---|---|

**超时动作枚举（P2）：**
| 值 | 说明 |
|---|---|
| ESCALATE | 转派给 escalate_to_user_id 或跳过节点 |
| AUTO_APPROVE | 自动通过当前节点 |
| AUTO_REJECT | 自动驳回，终止流程 |

---

|---|---|---|---|
| field_label | VARCHAR(100) | NOT NULL | 表示名（中国語） |
| create_time | DATETIME | NOT NULL | 作成日時 |

### 3.4 template_field（模板表单字段表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 字段 ID |
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属模板 |
| field_name | VARCHAR(50) | NOT NULL | 字段名（英文） |
| field_label | VARCHAR(100) | NOT NULL | 显示名（中文） |
| field_type | VARCHAR(30) | NOT NULL | 字段类型 |
| required | BIT | NOT NULL, DEFAULT 1 | 是否必填 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 排序号 |
| options | VARCHAR(500) | | SELECT 类型的 JSON 选项 |
| create_time | DATETIME | NOT NULL | 创建时间 |

**唯一制約：** `(template_id, field_name)`

**唯一约束：** `(template_id, field_name)`

**字段类型枚举：** TEXT / DATE / SELECT / TEXTAREA / NUMBER

---

|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 申請 ID |
| applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申請者 ID |
| start_date | DATE | NOT NULL | 開始日 |
| end_date | DATE | NOT NULL | 終了日 |
| reason | VARCHAR(500) | | 休暇理由 |
| create_time | DATETIME | NOT NULL | 提出日時 |
| update_time | DATETIME | NOT NULL | 更新日時 |

### 3.5 leave_request（请假申请表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 申请 ID |
| applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申请人 ID |
| template_id | BIGINT | FK → approval_template.id | 关联的审批模板 |
| leave_type | VARCHAR(20) | NOT NULL | 请假类型 |
| start_date | DATE | NOT NULL | 开始日期 |
| end_date | DATE | NOT NULL | 结束日期 |
| reason | VARCHAR(500) | | 请假原因 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 状态 |
| current_node_id | BIGINT | FK → approval_node.id | 当前审批节点 ID |
| current_approver_id | BIGINT | FK → sys_user.id | 当前审批人 ID（SINGLE 模式） |
| timeout_time | DATETIME | | 超时截止时间（P2） |
| create_time | DATETIME | NOT NULL | 提交时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

| 値 | 説明 |
|---|---|
| PENDING | 承認中 |

**状态枚举：**
| 值 | 说明 |
|---|---|
| PENDING | 审批中 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |
| WITHDRAWN | 已撤回 |

---

|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 記録 ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
| create_time | DATETIME | NOT NULL | 操作日時 |

### 3.6 approval_record（审批记录表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 记录 ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 关联申请 ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 审批人 ID（0=系统自动操作） |
| action | VARCHAR(20) | NOT NULL | 操作类型 |
| comment | VARCHAR(500) | | 审批意见 |
| node_id | BIGINT | FK → approval_node.id | 操作时的节点 ID |
| create_time | DATETIME | NOT NULL | 操作时间 |

| 値 | 説明 |
|---|---|
| APPROVE | 承認 |
| REJECT | 却下 |
| TRANSFER | 転送 |

**操作类型枚举：**
| 值 | 说明 |
|---|---|
| APPROVE | 通过 |
| REJECT | 驳回 |
| WITHDRAW | 撤回 |
| TRANSFER | 转派 |
| TIMEOUT_ESCALATE | 超时转派 |
| TIMEOUT_APPROVE | 超时自动通过 |
| TIMEOUT_REJECT | 超时自动驳回 |

---

|---|---|---|---|
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |

### 3.7 approval_task（并行审批任务表 — P2 新增）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 任务 ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 关联申请 ID |
| node_id | BIGINT | NOT NULL, FK → approval_node.id | 关联节点 ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 审批人 ID |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 任务状态 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**唯一制約：** `(leave_request_id, node_id, approver_id)`

**唯一约束：** `(leave_request_id, node_id, approver_id)`

| 値 | 説明 |
|---|---|

**任务状态枚举：**
| 值 | 说明 |
|---|---|
| PENDING | 待审批 |
| COMPLETED | 已审批 |
| SKIPPED | 已跳过（他人操作导致） |

---

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

## 4. 索引设计
| 表 | 索引类型 | 字段 |
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

|---|---|

## 5. 迁移脚本
| 脚本 | 说明 |
|---|---|
| `docs/mysql-p0-upgrade.sql` | P0 初始化：建库 + 4 表 + 种子数据 |
| `docs/mysql-p1-upgrade.sql` | P1 升级：approval_node + template_field + 新字段 |
| `docs/mysql-p2a-bcrypt.sql` | BCrypt 密码迁移 |
| `docs/mysql-p2b-parallel.sql` | P2 并行审批：sign_type + approver_ids + approval_task 表 |
| `docs/mysql-p2c-timeout.sql` | P2 超时升级：timeout_hours + timeout_action + escalate_to_user_id + timeout_time |
| `docs/mysql-p3-expense.sql` | v2.0 经费报销+复式记账：account/journal_entry/expense_request/expense_approval_task/audit_log |

---

文档版本：v2.0（P2 完成） | 更新日期：2026-05-27
