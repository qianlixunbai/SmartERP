# ADR-001：不可变审批模板版本模型

## 元数据

| 属性 | 值 |
|------|-----|
| **状态** | 已采纳 |
| **日期** | 2026-07-10 |
| **决策者** | SmartERP v4.0.1 稳定化工作组 |
| **影响范围** | 审批模板生命周期、请求绑定、数据迁移 |

---

## Context（背景与问题）

当前 SmartERP `approval_template` 存在以下问题：

1. **ACTIVE 模板可以原地修改**：模板名称、描述可随时更改，已提交申请的审批人看到的是修改后的模板信息，而非提交时的模板信息。
2. **节点保存会删除重建**：编辑模板节点时，旧节点被删除后重新插入，导致 `leave_request.current_node_id` 和 `approval_record.node_id` 指向已不存在的节点。
3. **历史 nodeId 会被清空**：审批完成后 `current_node_id` 被置为 NULL，丢失了审批流拓扑信息。
4. **模板和节点可以硬删除**：没有生命周期约束，正在使用的模板和节点可能被直接 DELETE。
5. **经费请求不保存模板版本**：`expense_request` 表没有 `template_id`，无法追溯该申请提交时使用的是哪个模板版本。
6. **经费固定使用模板 ID 1**：`ExpenseService` 硬编码 `templateId=1L`，无法支持多经费模板。
7. **进行中申请可能读取变化后的节点**：模板编辑后，进行中申请的审批流程可能指向已变更的节点。

---

## Decision（决定）

### 版本模型（方案 A）

`approval_template` 每一行代表一个**具体且最终不可变的模板版本**。

- 逻辑模板由 `template_key` 标识；
- 具体版本由 `template_key + version_no` 唯一确定；
- 所有请求表中的 `template_id` 最终都表示具体 `approval_template.id`，而非逻辑模板 ID。

### 生命周期

最终生命周期为三态：`DRAFT → ACTIVE → RETIRED`。

- `DRAFT`：草稿，可修改，不对外可见；
- `ACTIVE`：已发布，不可修改、不可硬删除；
- `RETIRED`：已退役，不可修改、不可硬删除，不可用于新申请。

### 唯一版本约束

- 同一 `(template_key, version_no)` 唯一；
- 同一 `template_key` 最多一个 `ACTIVE`；
- 同一 `template_key` 最多一个 `DRAFT`。

使用 MySQL 生成列（`active_slot`、`draft_slot`）和唯一索引实现数据库最终防线。

### 关键行为约定

1. **每个版本是一条 `approval_template`**：版本之间通过 `supersedes_id` 建立链式追溯。
2. **`template_id` 表示具体版本**：外键引用 `approval_template.id`，非逻辑模板 ID。
3. **ACTIVE/RETIRED 最终不可修改、不可硬删除**：通过 `lifecycle_status` 和 Service 层守卫实现。
4. **请求提交后永远读取固定版本**：提交时拷贝 `template_id`，审批过程中不再重新读取模板。
5. **经费将增加 templateId**：`expense_request.template_id` 用于保存提交时使用的模板版本。
6. **历史 nodeId 永不因模板编辑而清空**：节点数据保留，新建版本时创建新节点行。
7. **使用 MySQL 生成列唯一索引**：`active_slot` 和 `draft_slot` 生成列确保数据库层面唯一性约束。
8. **迁移采用 expand → backfill → enforce** 策略：5A 只执行 expand（新增列和索引），5B/5C 执行 backfill 和 enforce。
9. **现有 `enabled` 字段暂时保留**：待生命周期切换稳定后再处理兼容收缩。

---

## Consequences（影响与后果）

### 短期（5A）

- 只扩展 schema：新增列、生成列、索引。
- 不修改 Service/Controller/前端。
- 不发布版本、不创建 DRAFT、不退休 ACTIVE。
- 不改变审批推进逻辑。
- 现有请假和经费业务继续使用 `enabled` 字段。

### 中期（5B）

- 实现生命周期 Service：发布（DRAFT→ACTIVE）、退休（ACTIVE→RETIRED）、草稿化（DRAFT→DRAFT）。
- 回填现有模板的 `template_key`、`version_no`、`lifecycle_status`。
- 现有 `enabled=true` 的模板标记为 `ACTIVE`。

### 后期（5C）

- 切换请求绑定：提交时固定 `template_id`。
- 经费请求保存 `template_id`。
- 审批流程读取固定版本的节点。

### 最终（后续阶段）

- 添加最终 FK、NOT NULL 约束。
- 移除 `enabled` 字段。
- 数据收缩和清理。

### 兼容性

- 旧前端暂时可继续运行（未引入新 API）。
- 数据迁移必须显式处理 ID 1 和脏数据，不自动合并或修复。
- 本 ADR 生效于 v4.0.1 稳定化阶段，但**第五阶段尚未完成**——后续 5B、5C 将继续实施。
