# SmartOA — 简易 OA 审批流管理系统

企业级 OA 审批流管理系统 | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/Tests-24_passed-brightgreen" alt="Tests"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

---

## 项目简介

SmartOA 是一个面向企业日常办公的**简易审批流管理系统**，支持 JWT 认证、审批模板管理、请假申请与多级审批流转。核心设计围绕"模板配置 + 流程引擎"展开，支持条件分支、并行审批（会签/或签）、超时自动升级等高级特性。

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
smartoa/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/              # Result<T> 统一响应、BusinessException、GlobalExceptionHandler
│   │   ├── config/              # 安全配置、CORS、JWT 过滤器
│   │   ├── controller/          # REST 控制器（5 个）
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 实体类（7 个，含 ApprovalTask）
│   │   ├── mapper/              # MyBatis-Plus Mapper（7 个）
│   │   └── service/             # 业务逻辑层（5 个）+ TimeoutScheduler
│   ├── src/test/java/com/smartoa/service/
│   │   ├── LeaveServiceTest.java   # 审批流程测试（18 个用例）
│   │   └── UserServiceTest.java    # 用户登录测试（5 个用例）
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                 # 接口封装（auth / leave / template）
│       ├── stores/              # Pinia 状态管理（auth / approval / users）
│       ├── router/              # 路由配置
│       ├── views/               # 页面组件（15 个 Page）
│       ├── components/          # 共享组件
│       ├── styles/              # 全局 CSS 变量
│       └── layouts/             # 布局组件（MainLayout）
├── docs/                        # SQL 迁移脚本
├── CLAUDE.md
└── README.md
```

---

## 测试

项目包含 **24 个单元测试**，覆盖审批流程的核心场景。

```bash
# 运行所有测试
cd backend && ./mvnw test

# 只运行某个测试类
./mvnw test -Dtest=LeaveServiceTest
./mvnw test -Dtest=UserServiceTest

# 只运行某个测试方法
./mvnw test -Dtest=LeaveServiceTest#testAdminSubmitLeave_ShouldSkipDirectLeaderNode
```

### 测试覆盖

| 测试类 | 用例数 | 覆盖功能 |
|--------|--------|---------|
| LeaveServiceTest | 18 | 审批流程全部操作 |
| UserServiceTest | 5 | 登录与用户管理 |
| SmartoaApplicationTests | 1 | 应用启动 |

**LeaveServiceTest 详情：**
- 提交申请: 4个（admin跳过节点、员工正常、条件分支）
- 审批操作: 5个（通过、驳回、越权、重复、记录保存）
- 撤回: 3个（正常、非申请人、已驳回）
- 转派: 2个（正常、非审批人）
- 查询: 3个（详情、记录、待审批列表）
- 滞留修复: 1个

---

## 数据库设计

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（含直属领导、部门总监关联） |
| `approval_template` | 审批模板表 |
| `approval_node` | 审批节点表（支持条件表达式、签批模式、超时配置）⭐ 核心 |
| `template_field` | 模板字段表 |
| `leave_request` | 请假申请表（current_node_id + timeout_time 驱动流转） |
| `approval_record` | 审批记录表 |
| `approval_task` | 并行审批任务表（会签/或签模式下各审批人状态） |

---

## 快速启动

### 环境要求

- Java 21+
- MySQL 8.0+
- Node.js 18+ / pnpm

### 1. 建库

```sql
CREATE DATABASE smartoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后依次导入 `docs/` 下的 SQL 脚本。

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

- [x] 8 张数据库表设计
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

### 测试

- [x] **单元测试** — JUnit 5 + Spring Boot Test，24 个测试用例
- [x] **审批流程测试** — 覆盖提交、审批、驳回、撤回、转派、查询
- [x] **异常测试** — 验证越权操作、重复操作、非法参数
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

---

## 许可证

MIT License

---

## 开发历史

本分支（`github-cn`）为纯中文最终交付版。如需查看完整开发历史（66 个提交，含中日双语文档），请访问 [`github`](https://github.com/qianlixunbai/SmartOA/tree/github) 分支。
