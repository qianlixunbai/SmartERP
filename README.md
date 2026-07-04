# SmartERP — 企业级 ERP 系统（OA 模块 v1.0）

企业级 ERP 系统 | HR · OA · Finance · Treasury · Portfolio · AI

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/Tests-41_passed-brightgreen" alt="Tests"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

> **SmartERP 正在持续演进为一体化企业管理平台，融合办公自动化、财务核算、资金管理和投资组合分析。**

---

## 模块架构

```
SmartERP
├── HR           员工 · 请假 · 考勤
├── OA           审批 · 公告 · 工作流
├── Finance      报销 · 总账 · 复式记账 · 财务报表
├── Treasury     银行账户 · 企业资金 · 现金流
├── Portfolio    ETF · 股票 · 收益率 · 夏普比率 · 最大回撤
└── AI Assistant
```

---

## 项目简介

SmartERP 是一个面向企业的**模块化 ERP 平台**，采用「OA → Finance → Treasury → Portfolio → Analytics → AI」渐进式架构。当前 **v1.0（OA 模块）** 已完成，包含 JWT 认证、多级审批引擎（条件分支/并行审批/超时升级）、请假管理、经费报销、复式记账等企业级功能。最大亮点是 **Treasury & Portfolio Management（企业资金与投资管理）**，整合企业现金头寸、资金流预测、投资组合资产配置与风险分析。核心设计围绕"模板配置 + 流程引擎"展开，后续将逐步扩展至财务核算、资金管理、投资组合、BI 分析和 AI 助手。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.14 |
| 持久层 | MyBatis-Plus 3.5.15 |
| 数据库 | MySQL 8.0 |
| 认证鉴权 | JWT（jjwt 0.13.0）+ BCrypt |
| 测试 | JUnit 5 + Spring Boot Test |
| 前端框架 | Vue 3.5（Composition API） |
| UI 组件库 | Element Plus 2.13.7 |
| 构建工具 | Vite 8 |
| 包管理 | pnpm |
| 状态管理 | Pinia |
| 路由 | Vue Router 5 |

---

## 项目结构

```
smarterp/
├── backend/
│   ├── src/main/java/com/smarterp/
│   │   ├── common/              # Result<T> 统一响应、BusinessException、GlobalExceptionHandler
│   │   ├── config/              # 安全配置、CORS、JWT 过滤器、乐观锁插件
│   │   ├── controller/          # REST 控制器（6 个）
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 实体类（11 个）
│   │   ├── mapper/              # MyBatis-Plus Mapper（11 个）
│   │   └── service/             # 业务逻辑层（6 个）+ TimeoutScheduler
│   ├── src/test/java/com/smarterp/service/
│   │   ├── LeaveServiceTest.java       # 审批流程测试（18 个用例）
│   │   ├── AccountingServiceTest.java  # 复式记账测试（11 个用例）
│   │   ├── ExpenseServiceTest.java     # 经费报销测试（6 个用例）
│   │   └── UserServiceTest.java        # 用户登录测试（5 个用例）
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                 # 接口封装
│       ├── stores/              # Pinia 状态管理
│       ├── router/              # 路由配置
│       ├── views/               # 页面组件（21 个）
│       ├── components/          # 共享组件
│       ├── styles/              # 全局 CSS 变量
│       └── layouts/             # 布局组件（MainLayout）
├── docs/                        # SQL 迁移脚本（v1.0）
├── CLAUDE.md
└── README.md
```

---

## 测试

项目包含 **41 个单元测试**，覆盖审批流程、经费报销、复式记账的核心场景。

```bash
# 运行所有测试
cd backend && ./mvnw test

# 只运行某个测试类
./mvnw test -Dtest=LeaveServiceTest
./mvnw test -Dtest=AccountingServiceTest
./mvnw test -Dtest=ExpenseServiceTest
./mvnw test -Dtest=UserServiceTest
```

### 测试覆盖

| 测试类 | 用例数 | 覆盖功能 |
|--------|--------|---------|
| LeaveServiceTest | 18 | 审批流程全部操作 |
| AccountingServiceTest | 11 | 复式记账（入账/冲销/试算平衡/科目余额） |
| ExpenseServiceTest | 6 | 经费报销（提交/撤回/驳回） |
| UserServiceTest | 5 | 登录与用户管理 |
| SmartERPApplicationTests | 1 | 应用启动 |

**AccountingServiceTest 详情：**
- 入账: 5个（正常、精度、零值、负值、不同科目）
- 冲销: 3个（正常、重复冲销、不存在分录）
- 试算平衡: 2个（多笔入账、入账+冲销后余额归零）
- 科目余额: 1个

**ExpenseServiceTest 详情：**
- 提交: 3个（正常、零值、负值）
- 撤回: 2个（正常、非申请人撤回）
- 驳回: 1个

---

## 数据库设计

共 12 张表：

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（含直属领导、部门总监关联） |
| `approval_template` | 审批模板表 |
| `approval_node` | 审批节点表（支持条件表达式、签批模式、超时配置）⭐ 核心 |
| `template_field` | 模板字段表 |
| `leave_request` | 请假申请表（current_node_id + timeout_time 驱动流转） |
| `approval_record` | 审批记录表 |
| `approval_task` | 并行审批任务表（会签/或签模式下各审批人状态） |
| `account` | 会计科目表（10条种子数据） |
| `journal_entry` | 会计分录表（复式记账核心，含乐观锁） |
| `expense_request` | 经费报销申请表（含乐观锁） |
| `expense_approval_task` | 经费审批并行任务表 |
| `audit_log` | 审计日志表（只追加，不可修改/删除） |

---

## 快速启动

### 环境要求

- Java 21+
- MySQL 8.0+
- Node.js 18+ / pnpm

### 1. 建库

```sql
CREATE DATABASE smarterp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后依次导入 `docs/` 下的 SQL 脚本：

```bash
mysql -u root -p123456 --default-character-set=utf8mb4 < docs/mysql-p0-upgrade.sql
mysql -u root -p123456 --default-character-set=utf8mb4 smarterp < docs/mysql-p1-upgrade.sql
mysql -u root -p123456 --default-character-set=utf8mb4 smarterp < docs/mysql-p2a-bcrypt.sql
mysql -u root -p123456 --default-character-set=utf8mb4 smarterp < docs/mysql-p2b-parallel.sql
mysql -u root -p123456 --default-character-set=utf8mb4 smarterp < docs/mysql-p2c-timeout.sql
mysql -u root -p123456 --default-character-set=utf8mb4 smarterp < docs/mysql-p3-expense.sql
```

### 2. 启动后端

```bash
cd backend && ./mvnw spring-boot:run
```

默认端口 `8080`。

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm run dev
```

默认端口 `5173`，已配置代理转发到后端。

### 4. 登录

浏览器打开 `http://localhost:5173`，使用以下账户登录：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | 123456 | MANAGER | 技术部经理（无直属领导） |
| zhangsan | 123456 | EMPLOYEE | 普通员工（直属领导=admin） |
| lisi | 123456 | EMPLOYEE | 产品部员工 |

---

## 已实现功能

### P0 基础功能

- [x] 用户登录（JWT + BCrypt + 角色区分）
- [x] 审批模板 CRUD
- [x] 请假申请提交
- [x] 硬编码二级审批流转
- [x] 我的待办 / 已办 / 我提交的
- [x] 审批详情页

### P1 升级功能

- [x] 可配置多级审批引擎（approval_node 表驱动，动态节点遍历）
- [x] 同意 / 拒绝 / 撤回 / 转派四种操作
- [x] 审批节点配置 UI（模板编辑时可添加/删除/拖拽排序节点）
- [x] 流程进度条（`el-steps`，动态节点状态）
- [x] 审批历史时间线（`el-timeline`，颜色标注操作类型）
- [x] ECharts 统计图表
- [x] 后端 Excel 导出（Apache POI）
- [x] `Result<T>` 统一响应 + BusinessException 全局异常处理

### P2 升级功能

- [x] **流程节点可视化编辑器** — 拖拽排序、动态添加/删除节点
- [x] **条件分支** — SpEL 表达式驱动（支持按请假天数 `days`、请假类型 `leaveType` 等条件分流）
- [x] **并行审批** — 单人（SINGLE）/ 会签（COUNTER_SIGN）/ 或签（OR_SIGN）三种签批模式
- [x] **超时自动升级** — ESCALATE（转派）/ AUTO_APPROVE（自动通过）/ AUTO_REJECT（自动驳回），`@Scheduled` 每 5 分钟检查
- [x] **滞留修复** — `repairStuckRequests()` 修复 `currentApproverId` 为 null 的异常滞留申请

### v1.0 经费报销 + 复式记账

- [x] **复式记账引擎** — 每笔报销自动生成借方/贷方分录，保证 SUM(debit) == SUM(credit)
- [x] **BigDecimal 精度控制** — DECIMAL(19,2)，`setScale(2, HALF_UP)`，金额不使用 double/float
- [x] **乐观锁** — `@Version` 注解 + `OptimisticLockerInnerInterceptor`，防止并发审批冲突
- [x] **状态机** — DRAFT → PENDING → APPROVED → POSTED（已入账不可修改，只能红字冲销）
- [x] **红字冲销** — 生成借贷互换的反向分录，`memo="冲销#原交易ID"`
- [x] **审计日志** — 只追加（UPDATE/DELETE 不可），记录全部操作（SUBMIT/APPROVE/REJECT/WITHDRAW/POST/REVERSE）
- [x] **审批流共用** — 复用 `approval_node` 表驱动，独立 `expense_approval_task` 任务表
- [x] **SpEL 条件分支** — `ExpenseConditionVars(amount, category)` 支持金额/类别条件路由
- [x] **入账自动化** — 审批通过时自动调用 `AccountingService.post()` 生成分录

### 单元测试

- [x] **41 个测试用例** — JUnit 5 + Spring Boot Test
- [x] **审批流程测试** — 覆盖提交、审批、驳回、撤回、转派、查询
- [x] **复式记账测试** — 入账精度、冲销正确性、试算平衡、异常校验
- [x] **经费报销测试** — 提交校验、状态机流转、权限验证
- [x] **数据一致性** — 使用 `@Transactional` 测试后自动回滚

---

## API 概要

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/login` | 登录 |
| GET | `/api/users` | 用户列表 |
| GET | `/api/templates` | 模板列表 |
| POST | `/api/templates` | 创建模板 |
| GET | `/api/templates/{id}/nodes` | 获取审批节点 |
| POST | `/api/templates/{id}/nodes` | 保存审批节点 |
| POST | `/api/leave/submit` | 提交请假 |
| POST | `/api/leave/approve` | 审批请假 |
| POST | `/api/leave/{id}/withdraw` | 撤回 |
| POST | `/api/leave/{id}/transfer` | 转派 |
| GET | `/api/leave/pending` | 待审批列表 |
| GET | `/api/leave/done` | 已审批列表 |
| GET | `/api/leave/my-requests` | 我的申请 |
| GET | `/api/leave/{id}` | 请假单详情 |
| GET | `/api/leave/{id}/records` | 审批记录 |
| GET | `/api/leave/{id}/tasks` | 并行审批任务 |
| POST | `/api/leave/repair` | 滞留修复 |
| GET | `/api/stats/summary` | 统计摘要 |
| GET | `/api/stats/export` | Excel 导出 |
| POST | `/api/expense/submit` | 提交经费报销 |
| POST | `/api/expense/approve` | 审批/驳回经费 |
| POST | `/api/expense/{id}/withdraw` | 撤回经费 |
| POST | `/api/expense/{id}/reverse` | 红字冲销（管理员） |
| GET | `/api/expense/my-expenses` | 我的报销记录 |
| GET | `/api/expense/pending` | 待审批经费 |
| GET | `/api/expense/all` | 全部经费（管理员） |
| GET | `/api/expense/{id}` | 经费详情 |
| GET | `/api/expense/{id}/audit-logs` | 审计日志 |
| GET | `/api/accounting/trial-balance` | 试算平衡表 |
| GET | `/api/accounting/balances` | 科目余额表 |

---

## Roadmap

| 版本 | 模块 | 目的 |
|:---:|------|------|
| v1.0 ✅ | **OA** | 验证审批流程引擎，建立复式记账基础 |
| v2.0 🚧 | **Accounting** | 解决企业财务核算，实现凭证与科目管理 |
| v3.0 📅 | **Treasury** | 统一管理现金流、银行账户和资金调拨 |
| v4.0 📅 | **Portfolio** | 管理闲置资金投资，提供收益率与风险分析 |
| v5.0 📅 | **Analytics** | 全数据汇总至 BI 可视化看板 |
| v6.0 📅 | **AI** | AI 辅助审批建议、财务分析与风险预测 |

> 详细规划请查看：[docs/ROADMAP.md](docs/ROADMAP.md)

---

## 许可证

MIT License

---

## 开发历史

本分支（`github-cn`）为纯中文最终交付版。如需查看完整开发历史（66 个提交，含中日双语文档），请访问 [`github`](https://github.com/qianlixunbai/SmartERP/tree/github) 分支。
