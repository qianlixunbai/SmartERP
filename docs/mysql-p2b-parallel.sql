-- P4: 并行审批 — 审批节点加签批模式 + 审批任务表
ALTER TABLE approval_node
  ADD COLUMN sign_type VARCHAR(20) NOT NULL DEFAULT 'SINGLE'
    COMMENT 'SINGLE=单人, COUNTER_SIGN=会签, OR_SIGN=或签',
  ADD COLUMN approver_ids VARCHAR(1000) DEFAULT NULL
    COMMENT '并行签批时的审批人ID列表，逗号分隔';

CREATE TABLE approval_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  leave_request_id BIGINT NOT NULL,
  node_id BIGINT NOT NULL,
  approver_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING/COMPLETED/SKIPPED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (leave_request_id) REFERENCES leave_request(id),
  FOREIGN KEY (node_id) REFERENCES approval_node(id),
  FOREIGN KEY (approver_id) REFERENCES sys_user(id),
  UNIQUE KEY uk_request_node_approver (leave_request_id, node_id, approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
