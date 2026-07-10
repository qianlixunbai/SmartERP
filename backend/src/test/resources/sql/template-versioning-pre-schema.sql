-- ============================================================
-- SmartERP — Template Versioning Migration Test Schema
-- ============================================================
-- Subset of production schema used by
-- TemplateVersionSchemaMigrationIntegrationTest.
--
-- Contains all tables referenced by the P9 expand migration:
--   approval_template, approval_node, template_field,
--   leave_request, expense_request, audit_log
--
-- Foreign keys intentionally restricted: we include the FKs
-- that exist in production (approval_node.template_id,
-- template_field.template_id, leave_request.template_id,
-- leave_request.current_node_id) but NOT cross-table FKs
-- that could block fixture inserts.
--
-- This schema represents a realistic pre-migration state
-- with historical fixture data representing legacy production.
-- ============================================================

-- ==================== approval_template ====================

CREATE TABLE IF NOT EXISTS approval_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled BIT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== approval_node ====================

CREATE TABLE IF NOT EXISTS approval_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    approver_type VARCHAR(30) NOT NULL,
    approver_id BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES approval_template(id),
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
    FOREIGN KEY (template_id) REFERENCES approval_template(id),
    UNIQUE KEY uk_template_field (template_id, field_name)
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
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES approval_template(id),
    FOREIGN KEY (current_node_id) REFERENCES approval_node(id)
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
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status)
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
    create_time DATETIME NOT NULL,
    INDEX idx_target (target_type, target_id),
    INDEX idx_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Fixture Data（代表已有生产数据）
-- ============================================================

-- 模板 1：启用（enabled=true）
INSERT INTO approval_template (id, name, description, enabled) VALUES
  (1, 'Leave Request', 'Employee leave approval template', 1);

-- 模板 2：禁用（enabled=false）
INSERT INTO approval_template (id, name, description, enabled) VALUES
  (2, 'Expense Report', 'Expense reimbursement template (disabled)', 0);

-- approval_node：模板 1 有 2 个节点
INSERT INTO approval_node (id, template_id, node_name, sort_order, approver_type, approver_id) VALUES
  (1, 1, 'Direct Leader Approval', 0, 'DIRECT_LEADER', NULL),
  (2, 1, 'Department Head Approval', 1, 'DEPARTMENT_HEAD', NULL);

-- template_field：模板 1 有 4 个字段
INSERT INTO template_field (template_id, field_name, field_label, field_type, required, sort_order, options) VALUES
  (1, 'leaveType',   'Leave Type',   'SELECT',   1, 0, '["Annual","Personal","Sick","Wedding","Other"]'),
  (1, 'startDate',   'Start Date',   'DATE',     1, 1, NULL),
  (1, 'endDate',     'End Date',     'DATE',     1, 2, NULL),
  (1, 'reason',      'Reason',       'TEXTAREA', 1, 3, NULL);

-- leave_request：引用模板 1 的请假单
INSERT INTO leave_request (id, applicant_id, template_id, leave_type, start_date, end_date,
                           reason, status, approval_step, current_node_id, current_approver_id)
VALUES
  (100, 2, 1, '年假', '2026-07-01', '2026-07-03', '休假旅游', 'PENDING', 0, 1, 1),
  (101, 3, 1, '事假', '2026-07-05', '2026-07-05', '处理私事', 'APPROVED', 2, NULL, NULL);

-- expense_request：没有 template_id 的经费单（代表历史数据）
INSERT INTO expense_request (id, applicant_id, category, amount, description,
                             status, transaction_id, current_node_id, current_approver_id,
                             approval_step, version, create_time, update_time)
VALUES
  (200, 2, '差旅', 1500.00, '北京出差往返机票',
   'PENDING', NULL, 1, 1, 0, 1, '2026-07-01 10:00:00', '2026-07-01 10:00:00'),
  (201, 3, '办公', 300.00, '办公用品采购',
   'APPROVED', 'txn-uuid-001', NULL, NULL, 2, 0, '2026-06-15 09:00:00', '2026-06-16 14:00:00');

-- audit_log：既有审计日志（无 node_id）
INSERT INTO audit_log (id, action, target_type, target_id, actor_id, detail, ip_address, create_time)
VALUES
  (1, 'SUBMIT', 'EXPENSE', 200, 2, '{"category":"差旅"}', '127.0.0.1', '2026-07-01 10:00:00'),
  (2, 'APPROVE', 'EXPENSE', 201, 1, '{"action":"通过"}', '127.0.0.1', '2026-06-15 14:00:00');
