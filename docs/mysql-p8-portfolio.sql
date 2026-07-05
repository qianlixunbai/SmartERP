-- ============================================================
-- SmartERP v4.0 — Portfolio（投资组合管理）
-- 用法：mysql -u root -p123456 --default-character-set=utf8mb4 smarterp < docs/mysql-p8-portfolio.sql
-- ============================================================

USE smarterp;

-- ==================== 新建表 ====================

-- 1. 投资组合表
CREATE TABLE IF NOT EXISTS portfolio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '组合名称',
    description VARCHAR(300) COMMENT '描述',
    base_currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '基准币种: USD/JPY/CNY',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 资产标的表
CREATE TABLE IF NOT EXISTS portfolio_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE COMMENT '标的代码: QQQM/VOO/VTI/CASH',
    name VARCHAR(200) NOT NULL COMMENT '标的名称',
    asset_type VARCHAR(20) NOT NULL COMMENT 'ETF/STOCK/BOND/CASH',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '币种',
    current_price DECIMAL(19,4) DEFAULT NULL COMMENT '最新价格',
    price_date DATE DEFAULT NULL COMMENT '价格日期',
    create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 持仓表
CREATE TABLE IF NOT EXISTS portfolio_holding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL COMMENT '组合ID',
    asset_id BIGINT NOT NULL COMMENT '资产标的ID',
    quantity DECIMAL(19,6) NOT NULL DEFAULT 0 COMMENT '持有数量（份额/股数）',
    avg_cost DECIMAL(19,4) NOT NULL DEFAULT 0 COMMENT '平均成本价',
    total_cost DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT '总成本',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_portfolio_asset (portfolio_id, asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 交易记录表
CREATE TABLE IF NOT EXISTS portfolio_trade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL COMMENT '组合ID',
    asset_id BIGINT NOT NULL COMMENT '资产标的ID',
    trade_type VARCHAR(10) NOT NULL COMMENT 'BUY/SELL',
    quantity DECIMAL(19,6) NOT NULL COMMENT '数量',
    price DECIMAL(19,4) NOT NULL COMMENT '成交价',
    total_amount DECIMAL(19,2) NOT NULL COMMENT '成交金额',
    fee DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT '手续费',
    trade_date DATE NOT NULL COMMENT '交易日期',
    memo VARCHAR(300) COMMENT '备注',
    create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 股息记录表
CREATE TABLE IF NOT EXISTS portfolio_dividend (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL COMMENT '组合ID',
    asset_id BIGINT NOT NULL COMMENT '资产标的ID',
    amount DECIMAL(19,2) NOT NULL COMMENT '股息金额',
    per_share DECIMAL(19,4) NOT NULL COMMENT '每份/每股股息',
    quantity DECIMAL(19,6) NOT NULL COMMENT '持有数量（除权日）',
    dividend_date DATE NOT NULL COMMENT '派息日',
    create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 种子数据 ====================

-- 投资组合
INSERT IGNORE INTO portfolio (id, name, description, base_currency, active, create_time, update_time) VALUES
(1, '主投资组合 (USD)', '企业闲置资金投资组合，以美元计价，主要配置美股指数ETF', 'USD', 1, NOW(), NOW());

-- 资产标的
INSERT IGNORE INTO portfolio_asset (id, symbol, name, asset_type, currency, current_price, price_date, create_time) VALUES
(1, 'QQQM', 'Invesco NASDAQ 100 ETF', 'ETF', 'USD', 220.0000, '2026-07-01', NOW()),
(2, 'VOO', 'Vanguard S&P 500 ETF', 'ETF', 'USD', 530.0000, '2026-07-01', NOW()),
(3, 'VTI', 'Vanguard Total Stock Market ETF', 'ETF', 'USD', 290.0000, '2026-07-01', NOW()),
(4, 'CASH', '现金等价物', 'CASH', 'USD', 1.0000, '2026-07-01', NOW());

-- 持仓（主组合持有 QQQM 15股 @ $200 / VOO 10股 @ $500 / VTI 20股 @ $270）
INSERT IGNORE INTO portfolio_holding (id, portfolio_id, asset_id, quantity, avg_cost, total_cost, create_time, update_time) VALUES
(1, 1, 1, 15.000000, 200.0000, 3000.00, NOW(), NOW()),
(2, 1, 2, 10.000000, 500.0000, 5000.00, NOW(), NOW()),
(3, 1, 3, 20.000000, 270.0000, 5400.00, NOW(), NOW());

-- 交易记录（3笔买入对应持仓）
INSERT IGNORE INTO portfolio_trade (id, portfolio_id, asset_id, trade_type, quantity, price, total_amount, fee, trade_date, memo, create_time) VALUES
(1, 1, 1, 'BUY', 15.000000, 200.0000, 3000.00, 1.50, '2026-04-15', '定投买入 QQQM', NOW()),
(2, 1, 2, 'BUY', 10.000000, 500.0000, 5000.00, 2.50, '2026-04-15', '定投买入 VOO', NOW()),
(3, 1, 3, 'BUY', 20.000000, 270.0000, 5400.00, 2.70, '2026-05-01', '定投买入 VTI', NOW());

-- 股息记录
INSERT IGNORE INTO portfolio_dividend (id, portfolio_id, asset_id, amount, per_share, quantity, dividend_date, create_time) VALUES
(1, 1, 1, 7.35, 0.4900, 15.000000, '2026-06-30', NOW()),
(2, 1, 2, 16.50, 1.6500, 10.000000, '2026-06-28', NOW()),
(3, 1, 3, 12.80, 0.6400, 20.000000, '2026-06-29', NOW());
