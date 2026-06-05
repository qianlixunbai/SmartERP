-- ============================================================
-- SmartOA P0 — MySQL 建库 + 建表 + 种子数据（一次性执行）
-- 用法：打开 MySQL Workbench，粘贴全部内容，执行
-- ============================================================

CREATE DATABASE IF NOT EXISTS smartoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartoa;

-- ==================== 建表 ====================

DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS leave_request;
DROP TABLE IF EXISTS approval_template;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    department VARCHAR(50),
    direct_leader_id BIGINT,
    department_head_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE approval_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled BIT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE leave_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_step INT NOT NULL DEFAULT 0,
    current_approver_id BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_request_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    approval_step INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (leave_request_id) REFERENCES leave_request(id),
    FOREIGN KEY (approver_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 种子数据 ====================
-- 组织层级：王经理(直属领导) → 张总监/李总监(部门总监)

INSERT INTO sys_user (username, password, real_name, role, department, direct_leader_id, department_head_id) VALUES
  ('admin',     '123456', '王经理',   'MANAGER',  '技术部', NULL, 4),
  ('zhangsan',  '123456', '张三',     'EMPLOYEE', '技术部', 1, 4),
  ('lisi',      '123456', '李四',     'EMPLOYEE', '产品部', 1, 5),
  ('zongjian1', '123456', '张总监',   'MANAGER',  '技术部', NULL, NULL),
  ('zongjian2', '123456', '李总监',   'MANAGER',  '产品部', NULL, NULL);
