package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("expense_request")
public class ExpenseRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("applicant_id")
    private Long applicantId;

    @TableField("category")
    private String category;

    @TableField("cost_center_id")
    private Long costCenterId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("description")
    private String description;

    @TableField("receipt_url")
    private String receiptUrl;

    @TableField("status")
    private String status;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("current_node_id")
    private Long currentNodeId;

    @TableField("current_approver_id")
    private Long currentApproverId;

    @TableField("approval_step")
    private Integer approvalStep;

    @TableField("timeout_time")
    private LocalDateTime timeoutTime;

    @Version
    @TableField("version")
    private Integer version;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // ======== P9 模板版本化新增字段 ========

    @TableField("template_id")
    private Long templateId;
}
