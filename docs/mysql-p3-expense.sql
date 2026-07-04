-- ============================================================
-- SmartOA P6 — 经费报销 + 复式记账
-- 用法：mysql -u root -p123456 smartoa < docs/mysql-p6-expense.sql
-- ============================================================

USE smartoa;

-- ==================== 建表 ====================

-- 1. 会计科目表
CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE COMMENT '科目编码: 1001=库存现金, 6001=办公费',
    name VARCHAR(50) NOT NULL COMMENT '科目名称',
    type VARCHAR(20) NOT NULL COMMENT 'ASSET/LIABILITY/EXPENSE/EQUITY',
    parent_id BIGINT DEFAULT NULL COMMENT '父科目ID',
    active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 会计分录表（核心）
CREATE TABLE IF NOT EXISTS journal_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL COMMENT 'UUID，同一笔交易共享',
    account_id BIGINT NOT NULL COMMENT '科目ID',
    debit DECIMAL(19,2) DEFAULT 0.00 COMMENT '借方金额',
    credit DECIMAL(19,2) DEFAULT 0.00 COMMENT '贷方金额',
    memo VARCHAR(200) COMMENT '摘要',
    created_by BIGINT NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL,
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_txn (transaction_id),
    INDEX idx_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 经费申请表
CREATE TABLE IF NOT EXISTS expense_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    category VARCHAR(50) NOT NULL COMMENT '费用类别: 差旅/办公/招待/交通/其他',
    amount DECIMAL(19,2) NOT NULL COMMENT '报销金额',
    description VARCHAR(500) COMMENT '费用说明',
    receipt_url VARCHAR(500) COMMENT '发票/收据附件URL',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/WITHDRAWN/POSTED',
    transaction_id VARCHAR(36) DEFAULT NULL COMMENT '入账后关联分录UUID',
    current_node_id BIGINT DEFAULT NULL COMMENT '当前审批节点ID',
    current_approver_id BIGINT DEFAULT NULL COMMENT '当前审批人ID',
    approval_step INT DEFAULT 0 COMMENT '当前审批步骤',
    timeout_time DATETIME DEFAULT NULL COMMENT '超时时间',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 经费审批并行任务表
CREATE TABLE IF NOT EXISTS expense_approval_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_request_id BIGINT NOT NULL COMMENT '经费申请ID',
    node_id BIGINT NOT NULL COMMENT '审批节点ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPLETED/SKIPPED',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_expense (expense_request_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 审计日志表（只追加，不删不改）
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL COMMENT '操作类型: SUBMIT/APPROVE/REJECT/WITHDRAW/POST/REVERSE',
    target_type VARCHAR(30) NOT NULL COMMENT '目标类型: EXPENSE/JOURNAL',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    actor_id BIGINT NOT NULL COMMENT '操作人ID',
    detail TEXT COMMENT '详情JSON',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    create_time DATETIME NOT NULL,
    INDEX idx_target (target_type, target_id),
    INDEX idx_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 科目初始数据 ====================

INSERT IGNORE INTO account (code, name, type, create_time) VALUES
('1001', '库存现金',     'ASSET',    NOW()),
('1002', '银行存款',     'ASSET',    NOW()),
('1221', '其他应收款',   'ASSET',    NOW()),
('2202', '应付账款',     'LIABILITY', NOW()),
('6001', '办公费',       'EXPENSE',  NOW()),
('6002', '差旅费',       'EXPENSE',  NOW()),
('6003', '招待费',       'EXPENSE',  NOW()),
('6004', '交通费',       'EXPENSE',  NOW()),
('6005', '其他费用',     'EXPENSE',  NOW()),
('4001', '实收资本',     'EQUITY',   NOW());
