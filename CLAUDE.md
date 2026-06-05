# SmartOA — 简易 OA 审批流管理系统

Spring Boot + Vue 3 + MyBatis-Plus，聚焦请假审批流程。

---

## 技术栈

- 后端：Spring Boot 3.5.14 + Java 21 + MyBatis-Plus 3.5.15 + MySQL
- 前端：Vue 3.5.34 + Vite 8.0 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2 + pnpm
- 认证：JWT (jjwt 0.13.0) + BCrypt (spring-security-crypto)

## 启动方式

```bash
# 后端
cd backend && ./mvnw spring-boot:run          # → localhost:8080

# 前端
cd frontend && pnpm run dev      # → localhost:5173（proxy /api → 8080）
```

## 当前进度

- **P0/P1 已完成**：JWT 认证、可配置审批引擎（approval_node 表驱动）、6 表架构、4 审批动作（同意/拒绝/撤回/转派）、Excel 导出、ECharts 统计
- **P2 已完成**：
  - 流程节点可视化编辑器（拖拽排序、动态添加/删除）
  - 条件分支（SpEL 表达式，支持按请假天数/类型等条件分流）
  - 并行审批 — 会签（COUNTER_SIGN）+ 或签（OR_SIGN）+ 单人（SINGLE）
  - 超时自动升级（ESCALATE/AUTO_APPROVE/AUTO_REJECT，@Scheduled 每 5 分钟检查）
- **P3 待做**：动态表单渲染、前端 Excel 导出、移动端适配

## 数据库迁移

| 脚本 | 说明 |
|------|------|
| `docs/mysql-p0-upgrade.sql` | 建库建表 + 种子数据 |
| `docs/mysql-p3-bcrypt.sql` | 密码升级为 BCrypt |
| `docs/mysql-p4-parallel.sql` | 并行审批（sign_type + approval_task 表） |
| `docs/mysql-p5-timeout.sql` | 超时自动升级（timeout_hours + timeout_time） |

## 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | 123456 | MANAGER | 技术部经理 |
| zhangsan | 123456 | EMPLOYEE | 普通员工（直属领导=admin） |

## 分支说明

| 分支 | 语言 | 用途 |
|------|------|------|
| `master` | 中文 | Gitee |
| `github` | 中日双语 | GitHub，日本面试用 |
| `github-cn` | 纯中文 | GitHub，国内面试/学校用 |
