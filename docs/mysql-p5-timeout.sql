-- P5: 超时自动升级 — 审批节点超时配置 + 请假单超时时间
ALTER TABLE approval_node
  ADD COLUMN timeout_hours INT DEFAULT NULL
    COMMENT '超时小时数，NULL=不启用',
  ADD COLUMN timeout_action VARCHAR(20) NOT NULL DEFAULT 'ESCALATE'
    COMMENT 'ESCALATE=转派, AUTO_APPROVE=自动通过, AUTO_REJECT=自动驳回',
  ADD COLUMN escalate_to_user_id BIGINT DEFAULT NULL
    COMMENT '超时转派目标用户ID';

ALTER TABLE leave_request
  ADD COLUMN timeout_time DATETIME DEFAULT NULL
    COMMENT '当前节点超时截止时间';
