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
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("action")
    private String action;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("actor_id")
    private Long actorId;

    @TableField("detail")
    private String detail;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("create_time")
    private LocalDateTime createTime;

    // ======== P9 模板版本化新增字段 ========

    @TableField("node_id")
    private Long nodeId;
}
