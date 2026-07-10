-- ============================================
-- SmartERP — Minimal Test Schema
-- ============================================
-- Subset of production schema for integration testing.
-- Contains only: approval_task, approval_record,
--                expense_approval_task, audit_log
--
-- Derived from:
--   docs/mysql-p0-upgrade.sql    (approval_record)
--   docs/mysql-p2b-parallel.sql  (approval_task)
--   docs/mysql-p3-expense.sql    (expense_approval_task, audit_log)
--
-- Foreign keys intentionally omitted to avoid
-- requiring the full 21-table production schema.
-- ============================================

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
