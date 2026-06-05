package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("leave_request_id")
    private Long leaveRequestId;

    @TableField("approver_id")
    private Long approverId;

    @TableField("action")
    private String action;

    @TableField("approval_step")
    private Integer approvalStep;

    @TableField("node_id")
    private Long nodeId;

    @TableField("comment")
    private String comment;

    @TableField("create_time")
    private LocalDateTime createTime;
}
