# SmartERP — 模块化企业 ERP 系统

OA · Finance · Treasury · Portfolio · Workflow Engine

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/Regression-379_passed-brightgreen" alt="Regression Tests"/>
  <img src="https://img.shields.io/badge/MySQL_Integration-38_passed-blue" alt="MySQL Integration Tests"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

> 当前产品版本：**v4.0** | 当前工程阶段：**v4.0.1 稳定化进行中**

---

## 项目简介

SmartERP 是一个基于 **Java 21 + Spring Boot 3 + Vue 3 + MySQL 8** 的模块化企业管理系统，围绕"模板配置 + 流程引擎"核心设计，覆盖审批工作流、财务核算与投资组合管理。

目前已实现 OA 审批、经费报销、复式记账、财务期间、成本/利润中心和投资组合管理。当前 v4.0.1 稳定化阶段重点验证权限边界、历史数据可追溯性、事务一致性和并发安全，具体包括：

- 安全与对象级授权
- HTTP 错误语义
- 请求参数验证
- MySQL 真实集成测试
- 不可变审批模板版本
- 数据库事务和并发控制

SmartERP 不只展示功能数量，也重点验证权限边界、历史数据可追溯性、事务一致性和并发安全。

---

## 模块架构

```
SmartERP
├── HR           ✅ 已实现 — 员工 · 请假 · 考勤
├── OA           ✅ 已实现 — 审批 · 公告 · 工作流
├── Finance      ✅ 已实现 — 报销 · 总账 · 复式记账 · 财务报表
├── Treasury     📅 规划中 — 银行账户 · 企业资金 · 现金流
├── Portfolio    ✅ 已实现 — ETF · 股票 · 收益率 · 夏普比率 · 最大回撤
├── Analytics    📅 规划中 — BI 看板 · 财务报表 · 预算分析
└── AI Assistant 📅 规划中 — 审批建议 · 财务分析 · 风险预测
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.14 |
| 持久层 | MyBatis-Plus 3.5.15 |
| 数据库 | MySQL 8.0 |
| 认证鉴权 | JWT（jjwt 0.13.0）+ BCrypt |
| 测试 | JUnit 5 + Spring Boot Test + Testcontainers |
| 前端框架 | Vue 3.5（Composition API） |
| UI 组件库 | Element Plus 2.13.7 |
| 构建工具 | Vite 8 |
| 包管理 | pnpm |
| 状态管理 | Pinia |
| 路由 | Vue Router 5 |

---

## 项目结构

```
SmartERP/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/              # Result<T> 统一响应、BusinessException、GlobalExceptionHandler
│   │   ├── config/              # 安全配置、CORS、JWT 过滤器、乐观锁插件
│   │   ├── controller/          # REST 控制器
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 实体类
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   ├── service/             # 业务逻辑层 + TimeoutScheduler
│   │   └── security/config/     # 安全相关配置
│   ├── src/test/java/com/smartoa/
│   │   ├── service/             # Service 层回归测试
│   │   └── integration/         # Testcontainers MySQL 集成测试
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                 # 接口封装
│       ├── stores/              # Pinia 状态管理
│       ├── router/              # 路由配置
│       ├── views/               # 页面组件
│       ├── components/          # 共享组件
│       ├── styles/              # 全局 CSS 变量
│       └── layouts/             # 布局组件（MainLayout）
├── docs/                        # SQL 迁移脚本、ADR、架构文档
└── README.md
```

---

## v4.0.1 稳定化亮点

当前稳定化阶段已完成以下基础设施和关键能力：

### Schema expand（模板版本化）

`approval_template` 已新增 `template_key`、`version_no`、`workflow_type`、`lifecycle_status`、`published_at`、`retired_at`、`supersedes_id`、`revision` 及生成列 `active_slot`、`draft_slot`。`expense_request` 已新增可空 `template_id`，`audit_log` 已新增可空 `node_id`。新增唯一索引 `uk_template_key_version`、`uk_active_slot`、`uk_draft_slot`。

### 已关闭的生命周期边界

- 新模板只能创建为 DRAFT v1
- ACTIVE、RETIRED、legacy 模板不可原地修改
- ACTIVE 通过复制产生下一版本 DRAFT
- 节点和字段复制到新版本
- 历史 `node_id` 不再因模板编辑被清空
- 被业务数据引用的节点和模板不能物理删除
- 所有草稿写操作先锁模板行（`SELECT ... FOR UPDATE`）
- 并发更新不会丢失 revision
- 并发复制最多创建一个 DRAFT
- 子记录复制失败整体回滚

### 受限条件表达式执行器

替换了早期的任意 SpEL 实现，改为仅允许批准的变量、操作符和表达式结构；非法表达式及非布尔结果失败关闭。

### 尚未完成

- DRAFT 发布为 ACTIVE
- 旧 ACTIVE 退休
- 历史模板 backfill
- 请假/经费提交时固定具体模板版本
- Controller/API 与前端生命周期操作
- 最终 FK / NOT NULL / `enabled` 收缩

---

## 测试

当前稳定化 CI 共执行 **417 个 testcase**：379 个隔离回归测试 + 38 个真实 MySQL 集成测试。

数字来源：[SmartERP Stabilization Tests](https://github.com/qianlixunbai/SmartERP/actions/workflows/stabilization-tests.yml) workflow，两个 Job 各跑不同的测试集。

```bash
# 运行所有测试
cd backend && ./mvnw test

# 只运行某个测试类
./mvnw test -Dtest=TemplateServiceDraftLifecycleTest
./mvnw test -Dtest=TemplateVersionSchemaMigrationIntegrationTest
```

### unit-regression（379 testcases / 0 failures / 0 errors / 0 skipped）

覆盖重点：

- JWT 与认证配置
- 对象级授权
- 请假和经费审批操作
- 拒绝清理与修复权限
- HTTP 状态码
- DTO 与方法参数校验
- 安全附件 URL
- 投资组合授权与价格逻辑
- 条件表达式执行器
- 模板草稿生命周期与不可变边界

### mysql-integration（38 testcases / 0 failures / 0 errors / 0 skipped）

| 测试类 | 用例数 | 说明 |
|--------|--------|------|
| StabilizationMapperIntegrationTest | 8 | Mapper SQL 验证 |
| TemplateVersionSchemaMigrationIntegrationTest | 26 | P9 expand 迁移、生成列、唯一索引、历史 ID 保持 |
| TemplateServiceConcurrencyIntegrationTest | 4 | `SELECT ... FOR UPDATE`、并发复制、revision、行锁阻塞、回滚 |

真实覆盖：MySQL 8.0.40（Testcontainers）、P9 expand 迁移、生成列和唯一索引、历史 ID 保持、Mapper SQL、`SELECT ... FOR UPDATE`、并发复制只创建一个 DRAFT、并发 revision 不丢失、行锁阻塞、节点复制失败整体回滚。

---

## 数据库设计

共 21 张表：

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（含直属领导、部门总监关联） |
| `approval_template` | 审批模板具体版本；支持 template_key、version_no 与 DRAFT/ACTIVE/RETIRED 生命周期 |
| `approval_node` | 审批节点表（支持条件表达式、签批模式、超时配置） |
| `template_field` | 模板字段表 |
| `leave_request` | 请假申请表（current_node_id + timeout_time 驱动流转） |
| `approval_record` | 审批记录表 |
| `approval_task` | 并行审批任务表（会签/或签模式下各审批人状态） |
| `account` | 会计科目表（10条种子数据） |
| `journal_entry` | 会计分录表（复式记账核心，含乐观锁） |
| `expense_request` | 经费报销申请表（含乐观锁）；已扩展可空 template_id，运行时固定版本绑定将在后续阶段切换 |
| `expense_approval_task` | 经费审批并行任务表 |
| `audit_log` | 审计日志表（只追加，不可修改/删除）；已扩展可空 node_id，用于保留审批节点审计语义 |
| `cost_center` | 成本中心表 |
| `profit_center` | 利润中心表 |
| `fiscal_period` | 财务期间表（OPEN/CLOSED 状态） |
| `period_balance` | 月结余额快照表 |
| `portfolio` | 投资组合表 |
| `portfolio_asset` | 资产标的表（ETF/股票/债券/现金） |
| `portfolio_holding` | 持仓表（加权平均成本） |
| `portfolio_trade` | 交易记录表（BUY/SELL） |
| `portfolio_dividend` | 股息记录表 |

---

## 快速启动

### 环境要求

- Java 21+
- MySQL 8.0+
- Node.js 18+ / pnpm

### 1. 配置环境变量

Spring Boot **不会**自动读取仓库根目录的 `.env` 文件。请使用以下三种方式之一配置环境变量：

#### 方式一：IntelliJ IDEA Run Configuration（推荐）

1. Run → Edit Configurations → SmartoaApplication
2. Environment variables 中添加：

```
SMARTERP_DB_URL=jdbc:mysql://localhost:3306/smarterp?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
SMARTERP_DB_USERNAME=root
SMARTERP_DB_PASSWORD=你的MySQL密码
SMARTERP_JWT_SECRET=你的至少32字符JWT密钥
```

#### 方式二：PowerShell 启动脚本

```powershell
# 1. 创建本地配置文件
cp .env.example .env

# 2. 编辑 .env，填入你的数据库密码和 JWT 密钥

# 3. 通过脚本启动（自动加载 .env 并校验）
.\scripts\start-dev.ps1
```

脚本会检查 `SMARTERP_JWT_SECRET` 是否存在且至少 32 字符，不会在控制台输出任何密钥。

#### 方式三：PowerShell 手动设置

```powershell
$env:SMARTERP_JWT_SECRET = "你的至少32字符JWT密钥"
$env:SMARTERP_DB_URL     = "jdbc:mysql://localhost:3306/smarterp?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8"
$env:SMARTERP_DB_USERNAME = "root"
$env:SMARTERP_DB_PASSWORD = "你的MySQL密码"
cd backend
.\mvnw.cmd spring-boot:run
```

**所需环境变量：**

| 变量 | 说明 | 必填 |
|------|------|------|
| `SMARTERP_DB_URL` | 数据库连接 URL | 是 |
| `SMARTERP_DB_USERNAME` | 数据库用户名 | 是 |
| `SMARTERP_DB_PASSWORD` | 数据库密码 | 是 |
| `SMARTERP_JWT_SECRET` | JWT 签名密钥（≥32 字符） | **所有环境必填** |
| `SMARTERP_JWT_EXPIRATION` | Token 过期时间（毫秒） | 否（默认 24h） |

> **JWT 密钥要求：** `SMARTERP_JWT_SECRET` 为所有环境（dev/prod/test）必填项，不得少于 32 字符，缺失或过短将导致启动失败。可使用 PowerShell 生成：`$bytes = New-Object byte[] 32; [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes); [Convert]::ToBase64String($bytes)`

### 2. 建库

```sql
CREATE DATABASE smarterp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后依次导入 `docs/` 下的 SQL 脚本：

```bash
# Windows PowerShell
Get-ChildItem docs/mysql-p*.sql | Sort-Object Name | ForEach-Object {
    Get-Content $_ | mysql -u root -p smarterp --default-character-set=utf8mb4
}

# Git Bash / Linux / macOS
for f in docs/mysql-p*.sql; do
    mysql -u root -p smarterp --default-character-set=utf8mb4 < "$f"
done
```

### 3. 启动后端

```bash
cd backend

# Windows (Maven Wrapper)
mvnw.cmd spring-boot:run

# Git Bash / Linux / macOS
./mvnw spring-boot:run
```

默认端口 `8080`。

### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm run dev
```

默认端口 `5173`，已配置代理转发到后端。

### 5. 登录

浏览器打开 `http://localhost:5173`，使用以下账户登录：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | （见数据库 `sys_user` 表） | MANAGER | 技术部经理（无直属领导） |
| zhangsan | （见数据库 `sys_user` 表） | EMPLOYEE | 普通员工（直属领导=admin） |
| lisi | （见数据库 `sys_user` 表） | EMPLOYEE | 产品部员工 |

> 默认密码由 `docs/mysql-p2a-bcrypt.sql` 初始化，请查看该脚本中的 BCrypt 哈希对应的明文密码。

---

## 已实现功能

### OA 审批引擎

- [x] 用户登录（JWT + BCrypt + 角色区分）
- [x] 审批模板 CRUD
- [x] 请假申请提交
- [x] 可配置多级审批引擎（approval_node 表驱动，动态节点遍历）
- [x] 同意 / 拒绝 / 撤回 / 转派四种操作
- [x] 审批节点配置 UI（模板编辑时可添加/删除/拖拽排序节点）
- [x] 流程进度条 + 审批历史时间线
- [x] 条件分支（受限条件表达式执行器，支持按请假天数、请假类型等条件分流）
- [x] 并行审批 — 单人（SINGLE）/ 会签（COUNTER_SIGN）/ 或签（OR_SIGN）三种签批模式
- [x] 超时自动升级 — ESCALATE（转派）/ AUTO_APPROVE（自动通过）/ AUTO_REJECT（自动驳回）
- [x] 滞留修复 — `repairStuckRequests()` 修复异常滞留申请
- [x] 后端 Excel 导出（Apache POI）
- [x] `Result<T>` 统一响应 + BusinessException 全局异常处理

### 经费报销 + 复式记账

- [x] 复式记账引擎 — 每笔报销自动生成借方/贷方分录
- [x] BigDecimal 精度控制 — DECIMAL(19,2)，不用 double/float
- [x] 乐观锁 — `@Version` 注解 + `OptimisticLockerInnerInterceptor`
- [x] 状态机 — DRAFT → PENDING → APPROVED → POSTED
- [x] 红字冲销 — 生成借贷互换的反向分录
- [x] 审计日志 — 只追加，记录全部操作
- [x] 审批流共用 — 复用 `approval_node` 表驱动
- [x] 条件分支 — `ExpenseConditionVars(amount, category)` 支持金额/类别条件路由
- [x] 入账自动化 — 审批通过时自动调用 `AccountingService.post()` 生成分录

### 财务核算扩展

- [x] 成本中心 — 经费/凭证关联成本中心，部门费用归集
- [x] 利润中心 — 利润核算的业务单元
- [x] 财务期间管理 — OPEN/CLOSED 状态控制，月结锁定
- [x] 月结 — 期末余额快照到 `period_balance`
- [x] 反月结 — 重新打开期间
- [x] 财务看板 — 按期间/成本中心/利润中心维度汇总

### 投资组合管理

- [x] 多组合管理（USD/JPY/CNY 基准币种）
- [x] 资产标的管理 — ETF/股票/债券/现金
- [x] 交易执行 — BUY/SELL，加权平均成本自动更新持仓
- [x] 持仓汇总 — 市值/浮动盈亏/盈亏百分比/资产占比
- [x] 股息管理
- [x] 资产配置 — ECharts 饼图可视化
- [x] 组合绩效 — 总收益率/年化收益率/最大回撤/夏普比率/波动率
- [x] 投资看板
- [x] 演示数据 — QQQM / VOO / VTI

---

## API 概要

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/login` | 登录 |
| POST | `/api/logout` | 登出 |
| GET | `/api/user/current` | 当前用户信息 |
| GET | `/api/users/list` | 用户列表 |
| GET | `/api/templates` | 模板列表 |
| GET | `/api/templates/{id}` | 模板详情 |
| POST | `/api/templates` | 创建模板 |
| PUT | `/api/templates/{id}` | 更新模板 |
| DELETE | `/api/templates/{id}` | 删除模板 |
| GET | `/api/templates/{id}/nodes` | 获取审批节点 |
| POST | `/api/templates/{id}/nodes` | 保存审批节点 |
| DELETE | `/api/templates/{id}/nodes/{nodeId}` | 删除审批节点 |
| GET | `/api/templates/{id}/fields` | 获取模板字段 |
| POST | `/api/leave/submit` | 提交请假 |
| POST | `/api/leave/approve` | 审批请假 |
| POST | `/api/leave/{id}/withdraw` | 撤回 |
| POST | `/api/leave/{id}/transfer` | 转派 |
| GET | `/api/leave/all` | 全部请假（管理员） |
| GET | `/api/leave/pending` | 待审批列表 |
| GET | `/api/leave/done` | 已审批列表 |
| GET | `/api/leave/my-requests` | 我的申请 |
| GET | `/api/leave/{id}` | 请假单详情 |
| GET | `/api/leave/{id}/records` | 审批记录 |
| GET | `/api/leave/{id}/tasks` | 并行审批任务 |
| POST | `/api/leave/repair` | 滞留修复 |
| GET | `/api/stats/avg-duration` | 平均审批耗时 |
| GET | `/api/stats/template-usage` | 模板使用统计 |
| GET | `/api/export/leaves` | 导出请假 Excel |
| POST | `/api/expense/submit` | 提交经费报销 |
| POST | `/api/expense/approve` | 审批/驳回经费 |
| POST | `/api/expense/{id}/withdraw` | 撤回经费 |
| POST | `/api/expense/{id}/reverse` | 红字冲销（管理员） |
| GET | `/api/expense/my-expenses` | 我的报销记录 |
| GET | `/api/expense/pending` | 待审批经费 |
| GET | `/api/expense/all` | 全部经费（管理员） |
| GET | `/api/expense/{id}` | 经费详情 |
| GET | `/api/expense/{id}/audit-logs` | 审计日志 |
| GET | `/api/expense/{id}/tasks` | 并行审批任务 |
| GET | `/api/accounting/trial-balance` | 试算平衡表 |
| GET | `/api/accounting/balances` | 科目余额表 |
| GET | `/api/accounting/cost-centers` | 成本中心列表 |
| POST | `/api/accounting/cost-centers` | 创建成本中心 |
| PUT | `/api/accounting/cost-centers/{id}` | 编辑成本中心 |
| GET | `/api/accounting/profit-centers` | 利润中心列表 |
| POST | `/api/accounting/profit-centers` | 创建利润中心 |
| PUT | `/api/accounting/profit-centers/{id}` | 编辑利润中心 |
| GET | `/api/accounting/periods` | 财务期间列表 |
| POST | `/api/accounting/periods/close` | 月结 |
| POST | `/api/accounting/periods/reopen` | 反月结 |
| GET | `/api/accounting/period-balances/{periodId}` | 期间余额快照 |
| GET | `/api/accounting/dashboard` | 财务看板数据 |
| GET | `/api/portfolio/portfolios` | 投资组合列表 |
| POST | `/api/portfolio/portfolios` | 创建投资组合 |
| GET | `/api/portfolio/assets` | 资产标的列表 |
| POST | `/api/portfolio/assets` | 创建资产标的 |
| PUT | `/api/portfolio/assets/{id}/price` | 更新资产价格 |
| GET | `/api/portfolio/holdings` | 持仓汇总 |
| GET | `/api/portfolio/trades` | 交易记录 |
| POST | `/api/portfolio/trades` | 执行交易 |
| GET | `/api/portfolio/dividends` | 股息记录 |
| POST | `/api/portfolio/dividends` | 记录股息 |
| GET | `/api/portfolio/allocation` | 资产配置 |
| GET | `/api/portfolio/performance` | 组合绩效 |
| GET | `/api/portfolio/dashboard` | 投资看板 |

---

## Roadmap

| 版本 | 模块 | 目的 |
|:---:|------|------|
| v1.0 ✅ | **OA** | 验证审批流程引擎，建立复式记账基础 |
| v2.0 ✅ | **Accounting** | 解决企业财务核算，实现凭证与科目管理 |
| v3.0 📅 | **Treasury** | 统一管理现金流、银行账户和资金调拨 |
| v4.0 ✅ | **Portfolio** | 管理闲置资金投资，提供收益率与风险分析 |
| v4.0.1 🚧 | **Stabilization** | 安全、事务、模板版本化稳定化（进行中） |
| v5.0 📅 | **Analytics** | 全数据汇总至 BI 可视化看板 |
| v6.0 📅 | **AI** | AI 辅助审批建议、财务分析与风险预测 |

> 详细规划请查看：[docs/ROADMAP.md](docs/ROADMAP.md) | 稳定化状态：[docs/STABILIZATION_STATUS.md](docs/STABILIZATION_STATUS.md)

---

## 许可证

MIT License

---

## 开发历史

本分支（`fix/v4.0.1-stabilization`）为 v4.0.1 稳定化开发分支。如需查看完整开发历史，请访问 [`github`](https://github.com/qianlixunbai/SmartERP/tree/github) 分支。
