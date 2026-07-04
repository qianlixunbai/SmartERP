# 系统架构设计文档

## SmartOA 审批流管理系统

---

## 1. 架构概览

```
┌─────────────────────────────────────────────────────────┐
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
│                      浏览器 (Client)                      │
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
│   approval_task (并行审批)                                │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 技術選定

## 2. 技术选型

|---|---|---|---|
| 認証 | JJWT | 0.13.0 | 軽量 JWT 実装 |
| 状態管理 | Pinia | 3.x | Vue 3 公式推奨 |

| 层级 | 技术 | 版本 | 选型理由 |
|---|---|---|---|
| 后端框架 | Spring Boot | 3.5.14 | 企业级 Java 标准框架 |
| ORM | MyBatis-Plus | 3.5.15 | 灵活查询 + Lambda 类型安全 + 自动 CRUD |
| 数据库 | MySQL | 8.x | 成熟稳定，支持 utf8mb4 |
| 认证 | JJWT | 0.13.0 | 轻量 JWT 实现 |
| 密码加密 | BCrypt | spring-security-crypto | 单向哈希，不可逆 |
| 前端框架 | Vue 3 | 3.5 | Composition API + 响应式 |
| 构建工具 | Vite | 8.x | 极速 HMR，ESM 原生 |
| UI 库 | Element Plus | 2.13.7 | 企业级 Vue 3 组件库 |
| 状态管理 | Pinia | 3.x | Vue 3 官方推荐 |
| 图表 | ECharts | 5.x | 功能全面，中文友好 |
| Excel | Apache POI | 5.3 | Java Excel 读写标准库 |
| 包管理 | pnpm | — | 快速、节省磁盘空间 |

---

## 3. 包结构

### 3.1 后端（backend/src/main/java/com/smartoa/）

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
├── SmartoaApplication.java       # 启动类 + @MapperScan + @EnableScheduling
├── common/
│   ├── Result.java                # 统一响应 {code, message, data}
│   ├── BusinessException.java     # 业务异常
│   └── GlobalExceptionHandler.java # @RestControllerAdvice 全局异常处理
├── config/
│   ├── JwtProperties.java        # JWT 密钥+过期时间配置
│   ├── JwtUtil.java              # Token 生成/验证/解析工具
│   ├── JwtFilter.java            # 请求认证过滤器
│   ├── UserContextHolder.java    # ThreadLocal 保存当前用户 ID
│   └── WebConfig.java            # CORS 配置 + Filter 注册
├── controller/
│   ├── UserController.java       # 登录/登出/用户列表
│   ├── LeaveController.java      # 请假申请/审批/撤回/转派/滞留修复/并行任务
│   ├── TemplateController.java   # 模板CRUD/节点/字段
│   ├── StatsController.java      # 统计数据
│   └── ExportController.java     # Excel 导出
├── service/
│   ├── UserService.java          # 用户业务逻辑
│   ├── LeaveService.java         # 审批引擎核心（含并行+超时+条件分支）
│   ├── TemplateService.java      # 模板+节点+字段管理
│   ├── StatsService.java         # 统计计算
│   ├── ExportService.java        # Excel 生成
│   └── TimeoutScheduler.java     # 超时定时检查（@Scheduled 5分钟）
├── entity/
│   ├── User.java                 # 用户实体 (sys_user)
│   ├── ApprovalTemplate.java     # 模板实体 (approval_template)
│   ├── ApprovalNode.java         # 节点实体 (approval_node) — 含签批模式+条件+超时
│   ├── TemplateField.java        # 字段实体 (template_field)
│   ├── LeaveRequest.java         # 申请实体 (leave_request) — 含 timeoutTime
│   ├── ApprovalRecord.java       # 记录实体 (approval_record)
│   └── ApprovalTask.java         # 并行审批任务实体 (approval_task)
├── mapper/
│   ├── UserMapper.java
│   ├── LeaveRequestMapper.java
│   ├── ApprovalRecordMapper.java
│   ├── ApprovalTemplateMapper.java
│   ├── ApprovalNodeMapper.java
│   ├── TemplateFieldMapper.java
│   └── ApprovalTaskMapper.java
└── dto/
    ├── LoginDTO.java              # 登录请求体
    └── LeaveSubmitDTO.java        # 请假提交请求体
```

### 3.2 前端（frontend/src/）

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
├── App.vue                        # 根组件 + 全局 provide
├── main.js                        # 入口：Pinia + Router + Element Plus
├── api/
│   ├── index.js                   # axios 实例 + 拦截器
│   ├── auth.js                    # 认证接口
│   ├── leave.js                   # 请假接口（含 tasks/repair）
│   ├── template.js                # 模板接口
│   └── stats.js                   # 统计接口
├── stores/
│   ├── auth.js                    # 认证状态（token/user/role）
│   ├── approval.js                # 审批数据状态（含 currentTasks）
│   └── users.js                   # 用户列表 + ID→姓名映射
├── router/
│   └── index.js                   # 路由配置 + 导航守卫
├── layouts/
│   └── MainLayout.vue             # 主布局（Header + Sidebar + Content）
├── components/
│   ├── AppHeader.vue              # 顶部栏（用户信息/登出）
│   ├── AppSidebar.vue             # 侧栏导航
│   ├── ApprovalTimeline.vue       # 审批时间线
│   └── StatusTag.vue              # 状态标签
├── views/
│   ├── LoginPage.vue              # 登录页
│   ├── SubmitApplicationPage.vue  # 提交申请页
│   ├── MyApprovalsPage.vue        # 我的审批页
│   ├── ApprovalDetailPage.vue     # 审批详情页（含并行审批人标签+超时警告）
│   ├── TemplateListPage.vue       # 模板列表页
│   ├── TemplateEditPage.vue       # 模板编辑页（含可视化流程编辑器）
│   └── StatsPage.vue              # 统计报表页
└── utils/
    └── constants.js               # 前端常量（状态/动作/签批模式/超时动作映射）
```

---

## 4. 核心设计

### 4.1 审批引擎流程

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
│ 提交申请  │────▶│ 读取模板节点  │────▶│ 评估条件分支(SpEL) │
└──────────┘     └──────────────┘     └────────┬─────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 解析审批人 (resolveApprovers) │
                                │ ├─ SINGLE → 单人             │
                                │ ├─ COUNTER_SIGN → 多人列表    │
                                │ └─ OR_SIGN → 多人列表        │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ SINGLE: 设 currentApproverId  │
                                │ 并行: 批量插入 approval_task   │
                                │ 设置 timeoutTime（如有配置）   │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 审批人操作                     │
                                │ ├─ APPROVE → 推进/检查并行     │
                                │ ├─ REJECT → 终止+跳过并行任务  │
                                │ ├─ WITHDRAW → 终止 (申请人)   │
                                │ └─ TRANSFER → 替换审批人(SINGLE)│
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 有下一节点？                  │
                                │ ├─ 是 → 流转，继续等待审批    │
                                │ └─ 否 → APPROVED，流程结束   │
                                └─────────────────────────────┘
```

  - `DIRECT_LEADER` → `applicant.directLeaderId`
  - `DEPARTMENT_HEAD` → `applicant.departmentHeadId`

**关键方法：**
- `advanceToNextNode()` — 找下一个满足条件的节点，解析审批人，处理 SINGLE/并行模式
- `resolveApprovers()` — 根据 `approverType` + `signType` 解析审批人列表
  - `DIRECT_LEADER` → `applicant.directLeaderId`
  - `DEPARTMENT_HEAD` → `applicant.departmentHeadId`
  - `SPECIFIC_USER` → SINGLE: `node.approverId`；并行: `node.approverIds` 逗号分隔
- `evaluateCondition()` — SpEL 表达式求值，支持 `leaveType`、`days`、`startDate`、`endDate` 变量
- `checkTimeouts()` — 扫描 `timeoutTime` 过期的 PENDING 申请
- `processTimeout()` — 执行超时动作（ESCALATE/AUTO_APPROVE/AUTO_REJECT）

### 4.2 认证流程

```
  │
  ▼
  ├─ /api/login → 通過
  └─ 検証成功 → UserContextHolder.setUserId() → Controller
```

```
用户登录 → POST /api/login → BCrypt 验证密码 → 生成 JWT
  │
  ▼
后续请求 → JwtFilter 拦截
  ├─ /api/login → 放行
  ├─ 无 Authorization 头 → 401
  ├─ Token 无效/过期 → 401
  ├─ 用户不存在 → 401
  └─ 验证通过 → UserContextHolder.setUserId() → Controller
```

### 4.3 并行审批模式

|---|---|---|

| 模式 | 推进条件 | 说明 |
|---|---|---|
| SINGLE | 单人审批 | 保持现有行为，`currentApproverId` 驱动 |
| COUNTER_SIGN | 全部同意 | 每个审批人创建 `approval_task`，全部 COMPLETED 后推进 |
| OR_SIGN | 任一同意 | 任一人 APPROVE → 推进，其余任务 SKIPPED |

### 4.4 超时自动升级

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
    → 查询 status=PENDING AND timeout_time < NOW()
      → processTimeout()
        ├─ ESCALATE → 转派或跳过节点
        ├─ AUTO_APPROVE → 自动通过
        └─ AUTO_REJECT → 自动驳回
```

### 4.5 前端姓名映射方案

```
       │
       ▼
App.vue provide('getUserName', id => userStore.getUserName(id))
       │
       ▼
```

```
后端返回数据只含 ID（applicantId, approverId）
       │
       ▼
App.vue provide('getUserName', id => userStore.getUserName(id))
       │
       ▼
各组件 inject('getUserName') → ID 转姓名显示
```

---

## 5. 安全设计

| 対策 | 説明 |
|---|---|

| 措施 | 说明 |
|---|---|
| JWT 认证 | 所有 /api/* 接口（除 /api/login）需 Bearer Token |
| 密码加密 | BCrypt 单向哈希，不可逆 |
| 密码保护 | @JsonIgnore 防止密码序列化返回前端 |
| CORS | 仅允许 http://localhost:5173 跨域 |
| 权限校验 | 模板管理/导出接口检查 role=MANAGER |
| 操作校验 | 撤回校验申请人，审批校验当前审批人/并行任务 |
| 统一响应 | `Result<T>` {code, message, data}，全局异常处理 |

---

## 6. 部署架构

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
│  开发环境 (单机)                     │
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

| 组件 | 端口 | 启动命令 |
|---|---|---|
| 前端 | 5173 | `pnpm run dev` |
| 后端 | 8080 | `cd backend && ./mvnw spring-boot:run` |
| 数据库 | 3306 | MySQL 服务 |

---

文档版本：v2.0（P2 完成） | 更新日期：2026-05-27
```