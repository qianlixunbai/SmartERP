# SmartOA 简易审批流管理系统 — 完整技术文档

**版本：P2 完成版** | 日期：2026-05-27 | 技术栈：Spring Boot 3.5.14 + Vue 3 + MySQL 8.0 + JWT + MyBatis-Plus

---

## 一、数据库设计

系统共 8 张表，MySQL 8.0，字符集 utf8mb4，存储引擎 InnoDB。

### 1.1 用户表（sys_user）

存储用户信息，含组织层级关系（直属领导 + 部门总监）。

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

**种子数据**（所有用户密码：123456，BCrypt 加密）：

| ID | 用户名 | 姓名 | 角色 | 部门 | 直属领导 | 部门总监 |
|----|--------|------|------|------|----------|----------|
| 1 | admin | 王经理 | MANAGER | 技术部 | 无 | 4(张总监) |
| 2 | zhangsan | 张三 | EMPLOYEE | 技术部 | 1(王经理) | 4(张总监) |
| 3 | lisi | 李四 | EMPLOYEE | 产品部 | 1(王经理) | 5(李总监) |
| 4 | zongjian1 | 张总监 | MANAGER | 技术部 | 无 | 无 |
| 5 | zongjian2 | 李总监 | MANAGER | 产品部 | 无 | 无 |

### 1.2 审批模板表（approval_template）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| name | VARCHAR(100) | 模板名称 |
| description | VARCHAR(500) | 模板描述 |
| enabled | BIT | 是否启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 1.3 审批节点表（approval_node）⭐ 核心

模板下挂的审批步骤，由 `approval_template` 通过外键 `template_id` 1:N 关联。支持条件分支、并行审批、超时配置。

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

**条件表达式变量**：`leaveType`（请假类型 String）、`days`（请假天数 long）、`startDate`、`endDate`（LocalDate）

示例：`days > 3` → 超过 3 天的请假走此节点；`leaveType == '病假'` → 病假走此节点。

### 1.4 请假申请表（leave_request）

核心表。`current_node_id` + `current_approver_id` + `timeout_time` 驱动整个审批流转。

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

### 1.5 审批记录表（approval_record）

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

### 1.6 模板字段表（template_field）

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

### 1.7 并行审批任务表（approval_task）⭐ P2 新增

并行审批（会签/或签）模式下，跟踪每个审批人的审批状态。

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

约束：`UNIQUE (leave_request_id, node_id, approver_id)`

---

## 二、后端核心逻辑

### 2.1 JWT + BCrypt 认证流程

涉及文件：`JwtProperties.java`、`JwtUtil.java`、`UserContextHolder.java`、`JwtFilter.java`、`WebConfig.java`

**登录流程：**

1. 用户 `POST /api/login` 提交用户名密码
2. `UserService.login()` 查 `sys_user` 表，使用 `BCryptPasswordEncoder.matches()` 比对密码
3. `JwtUtil.generateToken()` 生成 JWT（sub=用户ID，claims 含 username+role，24h 过期）
4. 返回 `Result<Map>`：`{code:200, data:{token, user:{id, username, realName, role, department, directLeaderId, departmentHeadId}}}`

**请求认证流程：**

1. 每个 `/api/*` 请求经过 `JwtFilter.doFilterInternal()`
2. 从 `Authorization: Bearer xxx` 头部提取 token
3. jjwt 库验证签名 + 过期时间
4. 解析出 userId → 存入 `UserContextHolder`（ThreadLocal）
5. Controller 通过 `UserService.getLoginUser()` 获取当前用户
6. 请求结束后 `finally` 块清空 ThreadLocal，防止内存泄漏

### 2.2 审批流转引擎（LeaveService.java）⭐ 核心

#### 休暇申請送信（submitLeave）

#### 提交请假（submitLeave）

1. 获取申请人实体（含 directLeaderId、departmentHeadId）
2. 创建 LeaveRequest：status=PENDING, approvalStep=0
3. 调用 `advanceToNextNode()` 推进到第一个满足条件的审批节点
4. 入库，返回给前端

#### 承認操作（approveLeave）

#### 审批操作（approveLeave）

```
3. 承認者検証：

├── REJECT（却下）
│
├── APPROVE + OR_SIGN（或签）
│
├── APPROVE + COUNTER_SIGN（会签）
│
└── APPROVE + SINGLE（单人）

```

```
校验阶段：
1. 请假单存在？
2. 状态是 PENDING？
3. 审批人验证：
   - SINGLE 模式：审批人 == currentApproverId
   - 并行模式：审批人在 approval_task 表中有 PENDING 任务
   - 两者都不满足 → 拒绝
记录审批日志 → ApprovalRecord
流转判断：
├── REJECT（驳回）
│   ├── 状态 → REJECTED，清除 currentApproverId/currentNodeId/timeoutTime
│   └── 并行节点：跳过其余 PENDING 任务
│
├── APPROVE + OR_SIGN（或签）
│   ├── 完成当前审批人任务
│   ├── 跳过节点其余 PENDING 任务
│   └── 推进到下一节点
│
├── APPROVE + COUNTER_SIGN（会签）
│   ├── 完成当前审批人任务
│   ├── 检查是否所有人已完成
│   ├── 是 → 推进到下一节点
│   └── 否 → 等待其他人审批
│
└── APPROVE + SINGLE（单人）
    └── 推进到下一节点
推进到下一节点（advanceToNextNode）：
1. 加载模板所有节点（按 sortOrder 排序）
2. 从 currentNodeId 之后开始遍历
3. 对每个节点：
   a. 执行 SpEL 条件判断 → 不满足则跳过
   b. 解析审批人：
      - SINGLE → 按 approverType 解析（DIRECT_LEADER/DEPARTMENT_HEAD/SPECIFIC_USER）
      - 并行模式 → 解析 approverIds 逗号分隔列表
   c. 分配审批人：
      - SINGLE → 设置 currentApproverId
      - 并行模式 → currentApproverId=null，批量插入 approval_task
   d. 设置超时期限：
      - 如有 timeout_hours → timeout_time = now + timeout_hours
      - 否则 → timeout_time = null
   e. 返回 true
4. 无更多节点 → 返回 false（流程完成）
```

#### 其他操作

| 操作 | 方法 | 説明 |
|------|------|------|

| 操作 | 方法 | 说明 |
|------|------|------|
| 撤回 | `withdrawLeave()` | 只能撤回自己 PENDING 的申请，跳过当前节点 PENDING 任务 |
| 转派 | `transferLeave()` | 单人节点转派给其他用户（并行节点禁止转派） |
| 滞留修复 | `repairStuckRequests()` | 修复 currentApproverId 为 null 且无 PENDING 任务的滞留申请 |
| 超时检查 | `checkTimeouts()` | 查询 timeout_time 过期的申请，执行超时动作 |

#### 超时动作

| 動作 | 説明 |
|------|------|

| 动作 | 说明 |
|------|------|
| ESCALATE | 转派给指定人（有 escalate_to_user_id）或跳过进入下一节点 |
| AUTO_APPROVE | 跳过当前节点所有待审批任务，自动通过进入下一节点 |
| AUTO_REJECT | 驳回申请，终止流程 |

### 2.3 定时任务（TimeoutScheduler.java）

```java
@Scheduled(fixedRate = 300000) // 每 5 分钟
public void checkTimeouts() {
    int count = leaveService.checkTimeouts();
    if (count > 0) log.info("processed {} timed-out approvals", count);
}
```

### 2.4 审批模板管理（TemplateService.java）

- `listAll()` → 查询所有模板
- `getById(id)` → 查询单个模板，不存在抛 BusinessException
- `create(template)` → 创建模板
- `update(id, data)` → 更新 name/description/enabled
- `delete(id)` → 级联删除模板、节点、字段
- `listNodes(templateId)` → 查询模板的审批节点（按 sortOrder 排序）
- `saveNodes(templateId, nodes)` → 全量替换节点（先删旧节点，清理引用，再插新节点）
- `listFields(templateId)` → 查询模板的自定义字段

### 2.5 権限制御

### 2.5 权限控制

|------|----------|------|

| 模块 | 权限方式 | 说明 |
|------|----------|------|
| 模板管理 | Controller 层 `role == "MANAGER"` | 非 MANAGER 返回 403 |
| 审批操作 | Service 层 `currentApproverId` / `approval_task` | 任何人被指定为审批人即可批 |
| 前端路由 | `router.beforeEach` 角色守卫 | 非 MANAGER 访问 /templates 跳转主页 |
| CORS | WebConfig 允许 localhost:5173 | 开发环境跨域支持 |

### 2.6 统一响应格式

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

`BusinessException` + `@RestControllerAdvice`（GlobalExceptionHandler）全局统一异常处理。

### 2.7 接口清单（共 20+ 个）

| 序号 | 路径 | 方法 | 认证 | 説明 |
|------|------|------|------|------|
| 6 | `/api/leave/{id}/withdraw` | POST | 是 | 撤回 |
| 7 | `/api/leave/{id}/transfer` | POST | 是 | 転送 |
| 9 | `/api/leave/all` | GET | 是 | 全申請（MANAGER） |
| 13 | `/api/leave/{id}` | GET | 是 | 休暇申請詳細 |
| 14 | `/api/leave/{id}/records` | GET | 是 | 承認記録 |

| 序号 | 路径 | 方法 | 认证 | 说明 |
|------|------|------|------|------|
| 1 | `/api/login` | POST | 否 | 登录 |
| 2 | `/api/users` | GET | 是 | 用户列表 |
| 3 | `/api/users/current` | GET | 是 | 获取当前用户 |
| 4 | `/api/leave/submit` | POST | 是 | 提交请假申请 |
| 5 | `/api/leave/approve` | POST | 是 | 审批请假 |
| 6 | `/api/leave/{id}/withdraw` | POST | 是 | 撤回 |
| 7 | `/api/leave/{id}/transfer` | POST | 是 | 转派 |
| 8 | `/api/leave/repair` | POST | 是 | 修复滞留申请 |
| 9 | `/api/leave/all` | GET | 是 | 全部申请（MANAGER） |
| 10 | `/api/leave/my-requests` | GET | 是 | 我的申请 |
| 11 | `/api/leave/pending` | GET | 是 | 待审批列表 |
| 12 | `/api/leave/done` | GET | 是 | 已处理列表 |
| 13 | `/api/leave/{id}` | GET | 是 | 请假单详情 |
| 14 | `/api/leave/{id}/records` | GET | 是 | 审批记录 |
| 15 | `/api/leave/{id}/tasks` | GET | 是 | 并行审批任务 |
| 16 | `/api/templates` | GET | 是 | 模板列表 |
| 17 | `/api/templates/{id}` | GET | 是 | 模板详情 |
| 18 | `/api/templates` | POST | MANAGER | 创建模板 |
| 19 | `/api/templates/{id}` | PUT | MANAGER | 更新模板 |
| 20 | `/api/templates/{id}` | DELETE | MANAGER | 删除模板 |
| 21 | `/api/templates/{id}/nodes` | GET | 是 | 获取审批节点 |
| 22 | `/api/templates/{id}/nodes` | POST | MANAGER | 保存审批节点 |
| 23 | `/api/templates/{id}/fields` | GET | 是 | 获取模板字段 |
| 24 | `/api/stats/summary` | GET | 是 | 统计摘要 |
| 25 | `/api/stats/export` | GET | 是 | Excel 导出 |

---

## 三、前端架构

### 3.1 技术栈

Vue 3.5（Composition API）+ Element Plus 2.13.7 + Vite 8 + Pinia + Vue Router 5 + Axios + ECharts 5

Vue 3.5（Composition API）+ Element Plus 2.13.7 + Vite 8 + Pinia + Vue Router 5 + Axios + ECharts 5

入口文件 `main.js`：创建 Vue 应用 → 注册 Element Plus Icons → 安装 Pinia/Router/ElementPlus → 挂载 `#app`

### 3.2 布局系统

|------|------|----------|------|

| 布局 | 文件 | 使用场景 | 结构 |
|------|------|----------|------|
| AuthLayout | `layouts/AuthLayout.vue` | /login | 全屏居中，紫色渐变背景 |
| MainLayout | `layouts/MainLayout.vue` | 除登录外所有页面 | 左 220px 侧边栏 + 上 60px 导航 + 中间灰底内容区 |

### 3.3 路由配置

|------|------|------|------|
| `/submit-application` | SubmitApplicationPage.vue | 必須 | — |
| `/my-approvals` | MyApprovalsPage.vue | 必須 | — |
| `/approval/:id` | ApprovalDetailPage.vue | 必須 | — |
| `/templates` | TemplateListPage.vue | 必須 | MANAGER |
| `/templates/edit/:id?` | TemplateEditPage.vue | 必須 | MANAGER |
| `/stats` | StatsPage.vue | 必須 | — |
| `/:pathMatch(.*)*` | NotFoundPage.vue | — | 404 |

| 路径 | 页面 | 认证 | 角色 |
|------|------|------|------|
| `/login` | LoginPage.vue | 游客 | — |
| `/submit-application` | SubmitApplicationPage.vue | 必须 | — |
| `/my-approvals` | MyApprovalsPage.vue | 必须 | — |
| `/approval/:id` | ApprovalDetailPage.vue | 必须 | — |
| `/templates` | TemplateListPage.vue | 必须 | MANAGER |
| `/templates/edit/:id?` | TemplateEditPage.vue | 必须 | MANAGER |
| `/stats` | StatsPage.vue | 必须 | — |
| `/` | 重定向 | → /submit-application | — |
| `/:pathMatch(.*)*` | NotFoundPage.vue | — | 404 |

**路由守卫（beforeEach）：**

1. 有 token 但无 user → 自动调 `fetchUser()` 恢复登录态
2. 需要认证但未登录 → 跳转 `/login`
3. 已登录访问 `/login` → 跳转 `/submit-application`
4. 角色不匹配 → 跳转 `/submit-application`

### 3.4 Axios 封装（api/index.js）

- baseURL = `/api`（Vite 开发服务器代理到 localhost:8080）
- 请求拦截器：自动附加 `Authorization: Bearer <token>`
- 响应拦截器：`code === 200` → 直接返回 `data`；`code === 401` → 清空 token + 跳转登录；其他 → `ElMessage.error`

### 3.5 状態管理（Pinia）

### 3.5 状态管理（Pinia）

**auth store：**

**auth store：**

|-----------|------|

| 状态/方法 | 说明 |
|-----------|------|
| token | JWT 令牌，持久化到 localStorage |
| user | 用户信息对象，持久化到 localStorage |
| isManager | 计算属性：`user.role === "MANAGER"` |
| login() | 调登录接口 → 存 token+user → 跳转主页 |
| fetchUser() | GET /api/users/current 刷新当前用户 |
| logout() | 清空所有状态 → 跳转登录页 |

**approval store：**

**approval store：**

|-----------|------|
| fetchPendingRequests() | GET /api/leave/pending |
| fetchDoneRequests() | GET /api/leave/done |
| fetchDetail(id) | GET /api/leave/{id} |
| fetchRecords(id) | GET /api/leave/{id}/records |
| fetchTasks(id) | GET /api/leave/{id}/tasks |

| 状态/方法 | 说明 |
|-----------|------|
| pendingRequests / doneRequests / myRequests | 三种审批列表 |
| currentDetail / currentRecords / currentTasks | 当前请假单详情/记录/并行任务 |
| fetchPendingRequests() | GET /api/leave/pending |
| fetchDoneRequests() | GET /api/leave/done |
| fetchDetail(id) | GET /api/leave/{id} |
| fetchRecords(id) | GET /api/leave/{id}/records |
| fetchTasks(id) | GET /api/leave/{id}/tasks |

### 3.6 页面组件详解

**① 登录页（LoginPage.vue）**
- 紫色渐变全屏背景，白色居中 400px 卡片
- 用户名 + 密码输入框（带图标）
- 支持回车键快速登录
- 底部提示测试账号

**2. 申請送信（SubmitApplicationPage.vue）**

**② 提交申请（SubmitApplicationPage.vue）**
- 双栏布局：左侧表单 + 右侧我的请假记录
- 请假类型下拉（6 种）+ 日期选择器 + 原因文本域
- 前端校验：结束日期 >= 开始日期，必填项非空
- 提交成功：提示 + 清空表单 + 刷新列表

**③ 我的审批（MyApprovalsPage.vue）**
- 三 Tab 架构：待审批 / 已处理 / 我的申请
- 表格列：ID、申请人、请假类型、日期、状态、操作

**④ 审批详情（ApprovalDetailPage.vue）⭐ 核心页面**
- 顶部 `el-steps` 步骤条（动态节点 + 完成步骤）
- 并行审批：显示当前节点所有待审批人标签
- 超时提醒：显示超时截止时间（黄色警告栏）
- 详情区 `el-descriptions`（2 列带边框）
- 审批操作区（多人兼容）：
  - SINGLE 模式：`currentApproverId` 匹配显示
  - 并行模式：`approval_task` 中有 PENDING 任务显示
- 转派按钮 + 弹窗（仅 SINGLE 模式可用）
- 撤回按钮（仅申请人可见）
- 审批记录时间线

**⑤ 模板列表（TemplateListPage.vue）**
- 表格展示模板列表
- 新建/编辑/删除操作（MANAGER only）

**⑥ 模板编辑（TemplateEditPage.vue）⭐ P2 增强**
- 路由参数 `:id?` 可选
- 基本信息表单：模板名称 + 描述 + 启用开关
- **可视化流程编辑器**：
  - 拖拽手柄排序节点
  - 每个节点卡片包含：节点名称 + 签批模式（单人/会签/或签）+ 审批人类型/多人选择器
  - 条件表达式（SpEL）：可折叠展开，如 `days > 3`
  - 超时设置：可折叠展开，配超时小时数 + 超时动作（转派/自动通过/自动驳回）+ 目标用户
  - 删除按钮（hover 显示）
  - 节点间连接线

**⑦ 统计页（StatsPage.vue）**
- ECharts 图表：模板平均审批时长 + 各模板使用量

### 3.7 共享组件

|------|------|------|

| 组件 | 文件 | 功能 |
|------|------|------|
| LeaveTable | `LeaveTable.vue` | 通用审批表格 |
| LeaveForm | `LeaveForm.vue` | 请假表单封装 |
| ApprovalTimeline | `ApprovalTimeline.vue` | 审批时间线 |
| StatusTag | `StatusTag.vue` | 状态标签（PENDING=橙色/APPROVED=绿色/REJECTED=红色/WITHDRAWN=灰色） |

---

## 四、核心流程走查

### 4.1 单人审批流程（SINGLE）

以"张三请 1 天年假"为例：

|------|------|----------|

| 步骤 | 接口 | 数据处理 |
|------|------|----------|
| ① 张三登录 | POST /api/login | 获取 JWT；directLeaderId=1(王经理) |
| ② 提交请假 | POST /api/leave/submit | 创建 LeaveRequest，推进到直属领导节点（SINGLE），currentApproverId=1 |
| ③ 王经理查看待办 | GET /api/leave/pending | WHERE current_approver_id=1 AND status="PENDING" |
| ④ 王经理审批通过 | POST /api/leave/approve | requestId=1, action=APPROVE → 推进到下一节点 |
| ⑤ 重复③④ | — | 如还有节点继续流转，否则 APPROVED |

流转示意：

```
```

```
张三提交 → 直属领导（王经理）→ 条件判断 → 部门总监（如满足条件）→ 完成 ✓
```

### 4.2 会签流程（COUNTER_SIGN）

以"张三请 5 天年假，需 2 位总监会签"为例：

|------|------|

| 步骤 | 说明 |
|------|------|
| ① 提交 | 进入会签节点，插入 2 条 approval_task（张总监、李总监），currentApproverId=null |
| ② 张总监审批通过 | 完成自己的 task，检查 pendingCount>0 → 等待 |
| ③ 李总监审批通过 | 完成自己的 task，pendingCount=0 → 推进到下一节点 |

### 4.3 或签流程（OR_SIGN）

任何一人通过即推进：

|------|------|

| 步骤 | 说明 |
|------|------|
| ① 提交 | 进入或签节点，插入 N 条 approval_task |
| ② 任一人审批通过 | 完成自己的 task，其余 task → SKIPPED，立即推进 |

### 4.4 超时自动升级

   - AUTO_APPROVE → 自動承認
   - AUTO_REJECT → 自動却下

1. 节点配置 `timeout_hours=24`，进入节点时设置 `timeout_time = now + 24h`
2. `TimeoutScheduler` 每 5 分钟扫描：`WHERE status='PENDING' AND timeout_time <= now`
3. 执行配置的 timeout_action：
   - ESCALATE → 转派给指定人或跳过进入下一节点
   - AUTO_APPROVE → 自动通过
   - AUTO_REJECT → 自动驳回
4. 记录 `TIMEOUT_*` 审批日志

---

## 五、設定説明

## 五、配置说明

`application.properties` 主要設定：

`application.properties` 关键配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartoa
spring.datasource.username=root
spring.datasource.password=123456

# ========== MyBatis-Plus ==========
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

# ========== JWT ==========

# ========== CORS（WebConfig.java） ==========
```

```properties
# ========== 数据源（MySQL） ==========
spring.datasource.url=jdbc:mysql://localhost:3306/smartoa
spring.datasource.username=root
spring.datasource.password=123456
# ========== MyBatis-Plus ==========
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
# ========== JWT ==========
jwt.secret=SmartOA-Base64...（至少 256 位密钥）
jwt.expiration=86400000    ← 24 小时过期（毫秒）
# ========== CORS（WebConfig.java） ==========
允许 http://localhost:5173 跨域，允许 GET/POST/PUT/DELETE/OPTIONS
```

---

## 六、启动方式与测试账号

### 6.1 環境要件

### 6.1 环境要求

- JDK 21+
- MySQL 8.0
- Node.js 18+ / pnpm
- Maven 3.8+

- JDK 21+
- MySQL 8.0
- Node.js 18+ / pnpm
- Maven 3.8+

### 6.2 首次部署步骤

   ```sql
   CREATE DATABASE IF NOT EXISTS smartoa DEFAULT CHARACTER SET utf8mb4;
   ```

   ```
   docs/mysql-p4-parallel.sql  — 並行承認（sign_type + approval_task）
   ```

   ```bash
   cd backend && ./mvnw spring-boot:run        # → localhost:8080
   ```

   ```bash
   cd frontend
   pnpm install
   pnpm run dev                   # → localhost:5173
   ```

1. 创建数据库：
   ```sql
   CREATE DATABASE IF NOT EXISTS smartoa DEFAULT CHARACTER SET utf8mb4;
   ```
2. 依次执行迁移脚本：
   ```
   docs/mysql-p0-upgrade.sql   — 建表 + 种子数据
   docs/mysql-p3-bcrypt.sql    — BCrypt 密码迁移
   docs/mysql-p4-parallel.sql  — 并行审批（sign_type + approval_task）
   docs/mysql-p5-timeout.sql   — 超时自动升级
   ```
3. 启动后端：
   ```bash
   cd backend && ./mvnw spring-boot:run        # → localhost:8080
   ```
4. 启动前端：
   ```bash
   cd frontend
   pnpm install
   pnpm run dev                   # → localhost:5173
   ```

### 6.3 测试账号

|--------|------|------|------|------|
| lisi | 123456 | 李四 | EMPLOYEE | 製品部社員 |

| 用户名 | 密码 | 姓名 | 角色 | 用途 |
|--------|------|------|------|------|
| admin | 123456 | 王经理 | MANAGER | 经理审批 + 模板管理 |
| zhangsan | 123456 | 张三 | EMPLOYEE | 提交请假申请 |
| zongjian1 | 123456 | 张总监 | MANAGER | 总监审批 |
| lisi | 123456 | 李四 | EMPLOYEE | 产品部员工 |
| zongjian2 | 123456 | 李总监 | MANAGER | 产品部总监 |

**推荐测试流程：**

1. 用 `zhangsan` 登录 → 提交一条请假申请
2. 用 `admin` 登录 → 我的审批 → 待审批 → 通过
3. 用 `zhangsan` 登录 → 查看申请状态

---

## 七、项目结构

```
smartoa/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/                   # Result<T>、BusinessException、GlobalExceptionHandler
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── mapper/
│   │   └── service/
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                  # auth.js / leave.js / template.js
│       ├── components/
│       ├── layouts/              # AuthLayout / MainLayout
│       ├── router/               # index.js
│       ├── stores/               # auth.js / approval.js / users.js
│       ├── utils/                # constants.js
│       └── views/
├── docs/
├── CLAUDE.md
└── README.md
```

```
smartoa/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/                   # Result<T>、BusinessException、GlobalExceptionHandler
│   │   ├── config/                   # JWT 配置、CORS、过滤器
│   │   ├── controller/               # 5 个 REST 控制器
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 7 个实体类
│   │   ├── mapper/                   # 7 个 MyBatis-Plus Mapper
│   │   └── service/                  # 5 个 Service + TimeoutScheduler
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                  # auth.js / leave.js / template.js
│       ├── components/           # 6 个共享组件
│       ├── layouts/              # AuthLayout / MainLayout
│       ├── router/               # index.js
│       ├── stores/               # auth.js / approval.js / users.js
│       ├── utils/                # constants.js
│       └── views/                # 15 个 Page 组件
├── docs/                         # SQL 迁移脚本 + 技术文档
├── CLAUDE.md                     # 项目说明与开发进度
└── README.md                     # 项目 README
```

---

-- SmartOA P2 技術文書 · 完 --

— SmartOA P2 技术文档 · 完 —