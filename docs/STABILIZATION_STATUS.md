# SmartERP v4.0.1 稳定化状态

> 该文件记录工程稳定化状态，不替代产品 ROADMAP、数据库迁移脚本或 ADR。

---

## 1. 目标

在不破坏现有功能的前提下，系统性地加固 SmartERP 的安全边界、HTTP 语义、参数校验、数据库事务、并发控制和审批模板版本化，使系统具备可演进的工程基础。

---

## 2. 当前状态摘要

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1～3.5 | **CLOSED** | 安全、鉴权、HTTP、Portfolio 边界、测试基础 |
| Phase 4A | **CLOSED** | 受限条件表达式 |
| Phase 4B | **CLOSED** | 请求与路径参数校验 |
| Phase 4C | **CLOSED** | 附件 URL 安全 |
| Phase 5A | **CLOSED** | 模板版本 schema expand |
| Phase 5B-1 | **CLOSED** | DRAFT 不可变边界 |
| Phase 5B-2A | **CLOSED** | 行锁、事务与并发 |
| Phase 5B-2B | **PENDING** | 发布与退休 |
| Phase 5C | **PENDING** | 请求固定模板版本 |
| Phase 5D | **PENDING** | API 与前端过渡 |

---

## 3. 已完成阶段

### Phase 1～3：安全基础

- JWT 认证配置与属性校验
- 对象级授权（请假、经费审批的读取/审批权限分离）
- 拒绝清理（rejection 后正确清理审批状态）
- 修复权限（repair 操作仅管理员可执行）
- HTTP 状态码审计（401/403/404/409 语义对齐）
- JWT 过滤器 HTTP 响应不泄漏内部信息
- 数据库凭据和 JWT 密钥全部外部化，不在仓库中保存真实值；
- JWT 密钥缺失或少于 32 字符时启动失败关闭；
- 修正 `.env` 不会被 Spring Boot 自动加载造成的本地启动误区；
- 增加 PowerShell 开发启动脚本，加载本地 `.env` 并在启动前校验配置；
- 启动脚本对数据库密码和 JWT 密钥执行脱敏输出。

### Phase 3.5：Portfolio 管理员边界

- Portfolio 模块所有端点统一 requireManager() 守卫
- 资产价格更新纳入校验

### Phase 4A：受限条件表达式执行器

- 替换早期任意 SpEL 实现
- 仅允许批准的变量、操作符和表达式结构
- 非法表达式及非布尔结果失败关闭
- 不会造成表达式级别的权限绕过

### Phase 4B：请求参数与 PathVariable 校验

- DTO 层 `@Valid` + Jakarta Validation
- Controller 层 `@Validated` + `@Positive` 约束
- 参数校验失败返回统一 400 响应

### Phase 4C：安全附件 URL 约束

- 附件 URL 限制允许的协议（仅 http/https）
- 拒绝 file://、javascript: 等不安全 scheme

### Phase 5A：模板版本 schema expand

- `approval_template` 新增 8 个业务列 + 2 个生成列
- 新增 5 个索引（含 3 个唯一索引）
- `expense_request` 新增可空 `template_id` + 索引
- `audit_log` 新增可空 `node_id` + 索引
- 确保 `leave_request.template_id` 存在可用索引
- 不删除、不重命名、不重建现有列
- 不修改既有数据（仅 expand）

### Phase 5B-1：DRAFT 生命周期与历史引用保护

详见下方 [审批模板版本化进度](#4-审批模板版本化进度)。

### Phase 5B-2A：行锁、事务与并发

- 所有草稿写操作通过 `SELECT ... FOR UPDATE` 锁定模板行
- `createDraftFromVersion` 两级锁定：源行锁 + 同 key 全版本行锁
- 并发复制测试证明最多创建一个 DRAFT
- 并发更新测试证明 revision 不丢失
- 行锁阻塞测试证明 `FOR UPDATE` 确实阻塞第二个写事务
- 节点复制失败证明事务整体回滚

---

## 4. 审批模板版本化进度

### 4.1 设计依据

决策记录：[ADR-001：不可变审批模板版本模型](adr/ADR-001-immutable-approval-template-versions.md)

迁移脚本：[mysql-p9-template-versioning-expand.sql](mysql-p9-template-versioning-expand.sql)

核心约束：

- 不直接修改已采纳 ADR
- 当前仍处于 expand / Service foundation 阶段
- ACTIVE/RETIRED 不可修改
- 运行时固定模板版本尚未切换
- `enabled` 仍保留，待生命周期切换稳定后再处理兼容收缩
- 最终 FK 和 NOT NULL 尚未执行

### 4.2 Schema expand（已完成）

- `template_key`、`version_no`、`workflow_type`、`lifecycle_status`
- `published_at`、`retired_at`、`supersedes_id`、`revision`
- 生成列 `active_slot`、`draft_slot`
- 唯一索引 `uk_template_key_version`、`uk_active_slot`、`uk_draft_slot`
- `expense_request.template_id`（可空）
- `audit_log.node_id`（可空）

### 4.3 已关闭的生命周期边界

- 新模板只能创建为 DRAFT v1
- ACTIVE、RETIRED、legacy 模板不可原地修改
- ACTIVE 通过复制产生下一版本 DRAFT
- 节点和字段复制到新版本
- 历史 `node_id` 不再因模板编辑被清空
- 被业务数据引用的节点和模板不能物理删除
- 所有草稿写操作先锁模板行
- 并发更新不会丢失 revision
- 并发复制最多创建一个 DRAFT
- 子记录复制失败整体回滚

### 4.4 尚未完成

- DRAFT 发布为 ACTIVE
- 旧 ACTIVE 退休
- 历史模板 backfill
- 请假/经费提交时固定具体模板版本
- Controller/API 与前端生命周期操作
- 最终 FK / NOT NULL / `enabled` 收缩

---

## 5. CI 与测试证据

| 指标 | 值 |
|------|-----|
| 最后验证日期 | 2026-07-10 |
| 验证基线 | `a94defdd83ee47d5a579b828113f43d1aa515fa5` |
| GitHub Actions Run | [29103190131](https://github.com/qianlixunbai/SmartERP/actions/runs/29103190131) |

### unit-regression

```
testcases: 379
failures:   0
errors:     0
skipped:    0
```

覆盖：JWT 与认证配置、对象级授权、请假和经费审批操作、拒绝清理与修复权限、HTTP 状态码、DTO 与方法参数校验、安全附件 URL、投资组合授权与价格逻辑、条件表达式执行器、模板草稿生命周期与不可变边界。

### mysql-integration

```
testcases: 38
failures:   0
errors:     0
skipped:    0
```

| 测试类 | 用例数 |
|--------|--------|
| StabilizationMapperIntegrationTest | 8 |
| TemplateVersionSchemaMigrationIntegrationTest | 26 |
| TemplateServiceConcurrencyIntegrationTest | 4 |

- StabilizationMapperIntegrationTest：Spring Boot + MyBatis + Testcontainers；
- TemplateVersionSchemaMigrationIntegrationTest：纯 JDBC + Testcontainers；
- TemplateServiceConcurrencyIntegrationTest：最小 Spring Boot Context + MyBatis + Testcontainers。

真实覆盖：P9 expand 迁移、生成列和唯一索引、历史 ID 保持、Mapper SQL、`SELECT ... FOR UPDATE`、并发复制只创建一个 DRAFT、并发 revision 不丢失、行锁阻塞、节点复制失败整体回滚。

---

## 6. 尚未完成

以下阶段在相关测试和代码审查通过前不得标记为 CLOSED：

- **Phase 5B-2B**：发布（DRAFT→ACTIVE）和退休（ACTIVE→RETIRED）状态转换事务
- **Phase 5C**：请假/经费提交时固定具体模板版本；`expense_request.template_id` 写入
- **Phase 5D**：Controller/API 与前端过渡
- **Backfill**：回填现有模板的 `template_key`、`version_no`、`lifecycle_status`
- **Enforce**：最终 FK、NOT NULL 约束、`enabled` 字段移除

当前 Controller 尚未新增 publish/retire/draft 端点，前端尚未支持对应的生命周期操作界面。

---

## 7. 设计约束

- 数据库迁移采用 **expand → backfill → enforce** 三步策略，当前仅完成 expand
- 不直接修改已采纳的 ADR-001
- `enabled` 字段暂时保留，与 `lifecycle_status` 双轨运行
- 旧前端暂时可继续运行（未引入破坏性 API 变更）
- 数据迁移必须显式处理 ID 1 和脏数据，不自动合并或修复
- 仓库根目录 `.env` 仅作为本地配置来源，必须通过 IDE、启动脚本或系统环境变量注入 Spring Boot 进程。

---

## 8. 下一步

1. **Phase 5B-2B**：实现 DRAFT→ACTIVE 发布和 ACTIVE→RETIRED 退休的事务保护
2. **Phase 5C**：切换请求绑定逻辑，提交时固定 `template_id`
3. **Phase 5D**：Controller/API 与前端过渡
4. **Backfill**：回填历史数据
5. **Enforce**：添加最终约束，移除 `enabled`
6. **数据库设计文档同步**：在 5C/backfill/enforce 稳定后更新 `DATABASE_DESIGN.md`

---

> 更新日期：2026-07-10
