package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("approval_node")
public class ApprovalNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("node_name")
    private String nodeName;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("approver_type")
    private String approverType;

    @TableField("approver_id")
    private Long approverId;

    @TableField("condition_expression")
    private String conditionExpression;

    @TableField("sign_type")
    private String signType;

    @TableField("approver_ids")
    private String approverIds;

    @TableField("timeout_hours")
    private Integer timeoutHours;

    @TableField("timeout_action")
    private String timeoutAction;

    @TableField("escalate_to_user_id")
    private Long escalateToUserId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
