package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("applicant_id")
    private Long applicantId;

    @TableField("template_id")
    private Long templateId;

    @TableField("leave_type")
    private String leaveType;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("reason")
    private String reason;

    @TableField("status")
    private String status;

    @TableField("approval_step")
    private Integer approvalStep;

    @TableField("current_node_id")
    private Long currentNodeId;

    @TableField("current_approver_id")
    private Long currentApproverId;

    @TableField("timeout_time")
    private LocalDateTime timeoutTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
