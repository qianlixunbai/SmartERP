-- ============================================================
-- SmartERP P9 — 模板版本化扩展迁移（forward-only）
-- ============================================================
-- 用途：为审批模板版本化添加新列、生成列和唯一索引
-- 阶段：expand（仅扩展 schema，不回填数据，不强制执行）
-- 用法：mysql -u root -p smarterp < docs/mysql-p9-template-versioning-expand.sql
--
-- 安全要求：
--   1. 不删除或重命名任何列
--   2. 不修改既有数据（不执行 UPDATE）
--   3. 不重建现有表
--   4. 不添加跨表 FK
--   5. 不要求现有 template_key 非空
--   6. 不要求现有 expense_request.template_id 非空
--   7. 不依赖生产库为空
--   8. 新字段全部允许 NULL（后续 backfill/enforce 阶段再收紧）
--
-- 后续阶段（尚未执行）：
--   - backfill：回填 template_key、version_no、lifecycle_status
--   - enforce：添加 NOT NULL、CHECK 约束
-- ============================================================

USE smarterp;

-- ============================================================
-- 1. approval_template — 新增版本化字段
-- ============================================================

ALTER TABLE approval_template
    ADD COLUMN template_key VARCHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT '逻辑模板标识（同一逻辑模板的多个版本共享同一个 key）',
    ADD COLUMN version_no INT NULL
        COMMENT '版本号（从 1 开始递增）',
    ADD COLUMN workflow_type VARCHAR(20) NULL
        COMMENT '工作流类型（LEAVE/EXPENSE 等，用于模板分类路由）',
    ADD COLUMN lifecycle_status VARCHAR(16) NULL
        COMMENT '生命周期状态：DRAFT/ACTIVE/RETIRED',
    ADD COLUMN published_at DATETIME NULL
        COMMENT '发布时间（首次变为 ACTIVE 时写入）',
    ADD COLUMN retired_at DATETIME NULL
        COMMENT '退役时间（变为 RETIRED 时写入）',
    ADD COLUMN supersedes_id BIGINT NULL
        COMMENT '被本版本取代的旧版本 ID（版本链追溯）',
    ADD COLUMN revision INT NOT NULL DEFAULT 0
        COMMENT '乐观修订号（DRAFT 阶段可递增，ACTIVE/RETIRED 不再变化）';

-- ============================================================
-- 2. approval_template — 生成列（MySQL 5.7.6+ 支持）
-- ============================================================

-- active_slot：生命周期为 ACTIVE 时暴露 template_key，用于唯一约束
ALTER TABLE approval_template
    ADD COLUMN active_slot VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN lifecycle_status = 'ACTIVE' THEN template_key
                ELSE NULL
            END
        ) STORED
        COMMENT '生成列：ACTIVE 时 = template_key，否则 NULL';

-- draft_slot：生命周期为 DRAFT 时暴露 template_key，用于唯一约束
ALTER TABLE approval_template
    ADD COLUMN draft_slot VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN lifecycle_status = 'DRAFT' THEN template_key
                ELSE NULL
            END
        ) STORED
        COMMENT '生成列：DRAFT 时 = template_key，否则 NULL';

-- ============================================================
-- 3. approval_template — 新索引
-- ============================================================

-- 3a. 同一逻辑模板 + 同一版本号唯一
ALTER TABLE approval_template
    ADD UNIQUE INDEX uk_template_key_version (template_key, version_no);

-- 3b. 同一逻辑模板最多一个 ACTIVE
ALTER TABLE approval_template
    ADD UNIQUE INDEX uk_active_slot (active_slot);

-- 3c. 同一逻辑模板最多一个 DRAFT
ALTER TABLE approval_template
    ADD UNIQUE INDEX uk_draft_slot (draft_slot);

-- 3d. 按 template_key + lifecycle_status 查询
ALTER TABLE approval_template
    ADD INDEX idx_template_key_status (template_key, lifecycle_status);

-- 3e. 按 supersedes_id 追溯版本链
ALTER TABLE approval_template
    ADD INDEX idx_supersedes (supersedes_id);

-- ============================================================
-- 4. expense_request — 新增 template_id
-- ============================================================

ALTER TABLE expense_request
    ADD COLUMN template_id BIGINT NULL
        COMMENT '提交时使用的模板版本 ID（审批流程绑定到此版本）';

ALTER TABLE expense_request
    ADD INDEX idx_expense_template (template_id);

-- 注意：暂不添加 FK 或 NOT NULL，后续 enforce 阶段处理

-- ============================================================
-- 5. audit_log — 新增 node_id
-- ============================================================

ALTER TABLE audit_log
    ADD COLUMN node_id BIGINT NULL
        COMMENT '关联的审批节点 ID（用于审计追溯）';

ALTER TABLE audit_log
    ADD INDEX idx_audit_node (node_id);

-- 注意：暂不添加 FK，后续 enforce 阶段处理

-- ============================================================
-- 6. leave_request — 补充索引（如尚不存在）
-- ============================================================

-- leave_request.template_id 在 mysql-p1-upgrade.sql 中已存在，
-- 其外键会自动创建一个以 template_id 为首列的可用索引，索引名称不固定。
-- 仅在不存在任何此类前导索引时创建 idx_leave_template，避免冗余索引。
-- MySQL 不支持 CREATE INDEX IF NOT EXISTS，因此通过 INFORMATION_SCHEMA 安全检查。

-- 检查并添加 idx_leave_template
SET @sql_idx_leave_template = (
    SELECT IF(
        COUNT(DISTINCT INDEX_NAME) = 0,
        'ALTER TABLE leave_request ADD INDEX idx_leave_template (template_id)',
        'SELECT ''leave_request.template_id 已有可用索引，跳过'' AS msg'
    )
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'leave_request'
      AND COLUMN_NAME = 'template_id'
      AND SEQ_IN_INDEX = 1
);

PREPARE stmt FROM @sql_idx_leave_template;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 迁移后验证摘要
-- ============================================================

SELECT '========================================' AS '';
SELECT 'P9 扩展迁移执行完毕' AS '';
SELECT '' AS '';
SELECT '新增列（approval_template）：' AS '';
SELECT '  template_key, version_no, workflow_type, lifecycle_status,' AS '';
SELECT '  published_at, retired_at, supersedes_id, revision' AS '';
SELECT '  active_slot (GENERATED), draft_slot (GENERATED)' AS '';
SELECT '' AS '';
SELECT '新增索引（approval_template）：' AS '';
SELECT '  UNIQUE uk_template_key_version (template_key, version_no)' AS '';
SELECT '  UNIQUE uk_active_slot (active_slot)' AS '';
SELECT '  UNIQUE uk_draft_slot (draft_slot)' AS '';
SELECT '  INDEX idx_template_key_status (template_key, lifecycle_status)' AS '';
SELECT '  INDEX idx_supersedes (supersedes_id)' AS '';
SELECT '' AS '';
SELECT '新增列（expense_request）：template_id + idx_expense_template' AS '';
SELECT '新增列（audit_log）：node_id + idx_audit_node' AS '';
SELECT '确保 leave_request.template_id 存在可用索引' AS '';
SELECT '' AS '';
SELECT '后续步骤（尚未执行）：' AS '';
SELECT '  5B — backfill template_key/version_no/lifecycle_status' AS '';
SELECT '  5B — 实现生命周期 Service（DRAFT→ACTIVE→RETIRED）' AS '';
SELECT '  5C — 切换请求绑定逻辑' AS '';
SELECT '  最终 — 添加 NOT NULL、FK、CHECK 约束' AS '';
SELECT '========================================' AS '';
