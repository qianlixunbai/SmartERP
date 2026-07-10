-- ============================================================
-- SmartERP — 模板服务并发集成测试 Schema
-- ============================================================
-- 用于 TemplateServiceConcurrencyIntegrationTest。
--
-- 包含：
--   approval_template (完整 P9 生产列、生成列、索引)
--   approval_node (全部生产列)
--   template_field
--   approval_record, leave_request, approval_task
--   expense_request, expense_approval_task, audit_log
--
-- 不包含跨表外键（避免测试数据插入时的引用问题）。
-- ============================================================

-- ==================== approval_template ====================

CREATE TABLE IF NOT EXISTS approval_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled BIT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- P9 模板版本化字段
    template_key VARCHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT '逻辑模板标识',
    version_no INT NULL COMMENT '版本号',
    workflow_type VARCHAR(20) NULL COMMENT '工作流类型',
    lifecycle_status VARCHAR(16) NULL COMMENT '生命周期状态',
    published_at DATETIME NULL COMMENT '发布时间',
    retired_at DATETIME NULL COMMENT '退役时间',
    supersedes_id BIGINT NULL COMMENT '被本版本取代的旧版本 ID',
    revision INT NOT NULL DEFAULT 0 COMMENT '乐观修订号',

    -- P9 生成列
    active_slot VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN lifecycle_status = 'ACTIVE' THEN template_key
                ELSE NULL
            END
        ) STORED,
    draft_slot VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN lifecycle_status = 'DRAFT' THEN template_key
                ELSE NULL
            END
        ) STORED,

    -- P9 索引
    UNIQUE KEY uk_template_key_version (template_key, version_no),
    UNIQUE KEY uk_active_slot (active_slot),
    UNIQUE KEY uk_draft_slot (draft_slot),
    INDEX idx_template_key_status (template_key, lifecycle_status),
    INDEX idx_supersedes (supersedes_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== approval_node ====================

CREATE TABLE IF NOT EXISTS approval_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    approver_type VARCHAR(30) NOT NULL,
    approver_id BIGINT,
    condition_expression VARCHAR(500) DEFAULT NULL,
    sign_type VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    approver_ids VARCHAR(1000) DEFAULT NULL,
    timeout_hours INT DEFAULT NULL,
    timeout_action VARCHAR(20) NOT NULL DEFAULT 'ESCALATE',
    escalate_to_user_id BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_sort (template_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== template_field ====================

CREATE TABLE IF NOT EXISTS template_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    field_label VARCHAR(100) NOT NULL,
    field_type VARCHAR(30) NOT NULL,
    required BIT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    options VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_field (template_id, field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== approval_record ====================

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_request_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    approval_step INT NOT NULL DEFAULT 0,
    node_id BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== leave_request ====================

CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    template_id BIGINT,
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_step INT NOT NULL DEFAULT 0,
    current_node_id BIGINT,
    current_approver_id BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== approval_task ====================

CREATE TABLE IF NOT EXISTS approval_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_request_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_node_approver (leave_request_id, node_id, approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== expense_request ====================

CREATE TABLE IF NOT EXISTS expense_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    description VARCHAR(500),
    receipt_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    transaction_id VARCHAR(36) DEFAULT NULL,
    current_node_id BIGINT DEFAULT NULL,
    current_approver_id BIGINT DEFAULT NULL,
    approval_step INT DEFAULT 0,
    timeout_time DATETIME DEFAULT NULL,
    version INT DEFAULT 0,
    template_id BIGINT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status),
    INDEX idx_expense_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== expense_approval_task ====================

CREATE TABLE IF NOT EXISTS expense_approval_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_request_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_expense (expense_request_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== audit_log ====================

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    detail TEXT,
    ip_address VARCHAR(45),
    node_id BIGINT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_target (target_type, target_id),
    INDEX idx_actor (actor_id),
    INDEX idx_audit_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
