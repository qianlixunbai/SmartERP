-- ============================================================
-- SmartERP v2.0 — 成本中心 + 利润中心 + 财务期间 + 月结
-- 用法：mysql -u root -p123456 smarterp < docs/mysql-p7-accounting.sql
-- ============================================================

USE smarterp;

-- ==================== 新建表 ====================

-- 1. 成本中心表
CREATE TABLE IF NOT EXISTS cost_center (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '成本中心编码: CC001=研发部',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    description VARCHAR(300) COMMENT '描述',
    active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 利润中心表
CREATE TABLE IF NOT EXISTS profit_center (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '利润中心编码: PC001=华东区',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    description VARCHAR(300) COMMENT '描述',
    active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 财务期间表
CREATE TABLE IF NOT EXISTS fiscal_period (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL COMMENT '年度: 2026',
    month INT NOT NULL COMMENT '月份: 1-12',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    closed_by BIGINT DEFAULT NULL COMMENT '结账人ID',
    closed_time DATETIME DEFAULT NULL COMMENT '结账时间',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_year_month (year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 月结余额快照表
CREATE TABLE IF NOT EXISTS period_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL COMMENT '财务期间ID',
    account_id BIGINT NOT NULL COMMENT '会计科目ID',
    cost_center_id BIGINT DEFAULT NULL COMMENT '成本中心ID',
    profit_center_id BIGINT DEFAULT NULL COMMENT '利润中心ID',
    debit_total DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT '借方合计',
    credit_total DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT '贷方合计',
    closing_balance DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT '期末余额',
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_period_account_cc_pc (period_id, account_id, cost_center_id, profit_center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== ALTER 已有表 ====================

-- journal_entry 新增成本中心/利润中心关联
ALTER TABLE journal_entry ADD COLUMN cost_center_id BIGINT DEFAULT NULL COMMENT '成本中心ID' AFTER memo;
ALTER TABLE journal_entry ADD COLUMN profit_center_id BIGINT DEFAULT NULL COMMENT '利润中心ID' AFTER cost_center_id;

-- expense_request 新增成本中心关联
ALTER TABLE expense_request ADD COLUMN cost_center_id BIGINT DEFAULT NULL COMMENT '成本中心ID' AFTER category;

-- ==================== 种子数据 ====================

-- 成本中心
INSERT IGNORE INTO cost_center (code, name, description, active, create_time, update_time) VALUES
('CC001', '研发部', '技术研发', 1, NOW(), NOW()),
('CC002', '市场部', '市场营销', 1, NOW(), NOW()),
('CC003', '行政部', '行政后勤', 1, NOW(), NOW());

-- 利润中心
INSERT IGNORE INTO profit_center (code, name, description, active, create_time, update_time) VALUES
('PC001', '华东区', '华东大区', 1, NOW(), NOW()),
('PC002', '华南区', '华南大区', 1, NOW(), NOW()),
('PC003', '线上业务', '电商平台', 1, NOW(), NOW());

-- 初始财务期间（2026年1-6月已关闭，7月开放）
INSERT IGNORE INTO fiscal_period (year, month, status, create_time, update_time) VALUES
(2026, 1, 'CLOSED', NOW(), NOW()),
(2026, 2, 'CLOSED', NOW(), NOW()),
(2026, 3, 'CLOSED', NOW(), NOW()),
(2026, 4, 'CLOSED', NOW(), NOW()),
(2026, 5, 'CLOSED', NOW(), NOW()),
(2026, 6, 'CLOSED', NOW(), NOW()),
(2026, 7, 'OPEN', NOW(), NOW());
