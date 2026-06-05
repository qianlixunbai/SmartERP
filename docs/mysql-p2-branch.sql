-- P2: 条件分岐 — 审批节点に条件式フィールドを追加
ALTER TABLE approval_node ADD COLUMN condition_expression VARCHAR(500) DEFAULT NULL COMMENT 'SpEL条件表达式，null表示始终进入';
