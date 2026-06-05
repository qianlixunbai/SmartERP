-- ============================================================
-- SmartOA P1 — 可配置审批引擎升级脚本
-- 新增 approval_node + template_field，修改 leave_request / approval_record
-- 用法：MySQL Workbench 粘贴执行
-- ============================================================

USE smartoa;

-- ==================== 新表 ====================

-- 审批节点表：每个模板可配置任意级数审批节点
DROP TABLE IF EXISTS approval_node;
CREATE TABLE approval_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称，如"直属领导审批"',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '顺序，0起',
    approver_type VARCHAR(30) NOT NULL COMMENT '审批人类型：DIRECT_LEADER|DEPARTMENT_HEAD|SPECIFIC_USER|APPLICANT_SELF',
    approver_id BIGINT COMMENT 'approver_type=SPECIFIC_USER时指定用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES approval_template(id),
    UNIQUE KEY uk_template_sort (template_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模板表单字段表：每个模板定义自己的表单字段
DROP TABLE IF EXISTS template_field;
CREATE TABLE template_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    field_name VARCHAR(50) NOT NULL COMMENT '字段名，如 leaveType',
    field_label VARCHAR(100) NOT NULL COMMENT '显示名，如"请假类型"',
    field_type VARCHAR(30) NOT NULL COMMENT '字段类型：TEXT|DATE|SELECT|TEXTAREA|NUMBER',
    required BIT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    options VARCHAR(500) COMMENT 'SELECT类型的选项，JSON数组',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES approval_template(id),
    UNIQUE KEY uk_template_field (template_id, field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 修改现有表 ====================

-- leave_request：关联模板 + 当前审批节点
ALTER TABLE leave_request
    ADD COLUMN template_id BIGINT AFTER applicant_id,
    ADD COLUMN current_node_id BIGINT AFTER approval_step,
    ADD FOREIGN KEY (template_id) REFERENCES approval_template(id),
    ADD FOREIGN KEY (current_node_id) REFERENCES approval_node(id);

-- approval_record：关联审批节点
ALTER TABLE approval_record
    ADD COLUMN node_id BIGINT AFTER approval_step,
    ADD FOREIGN KEY (node_id) REFERENCES approval_node(id);

-- ==================== 种子数据 ====================

-- 审批模板
INSERT INTO approval_template (id, name, description, enabled) VALUES
  (1, '请假申请', '员工请假审批模板', 1);

-- 审批节点：2级 — 直属领导 → 部门总监
INSERT INTO approval_node (id, template_id, node_name, sort_order, approver_type, approver_id) VALUES
  (1, 1, '直属领导审批', 0, 'DIRECT_LEADER', NULL),
  (2, 1, '部门总监审批', 1, 'DEPARTMENT_HEAD', NULL);

-- 表单字段
INSERT INTO template_field (template_id, field_name, field_label, field_type, required, sort_order, options) VALUES
  (1, 'leaveType',   '请假类型', 'SELECT', 1, 0, '["年假","事假","病假","婚假","其他"]'),
  (1, 'startDate',   '开始日期', 'DATE',   1, 1, NULL),
  (1, 'endDate',     '结束日期', 'DATE',   1, 2, NULL),
  (1, 'reason',      '请假原因', 'TEXTAREA', 1, 3, NULL);

-- 更新已有的 leave_request 关联模板1（如果有历史数据）
UPDATE leave_request SET template_id = 1 WHERE template_id IS NULL;
