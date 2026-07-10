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
@TableName("approval_template")
public class ApprovalTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("enabled")
    private boolean enabled = true;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // ======== P9 模板版本化新增字段 ========

    @TableField("template_key")
    private String templateKey;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("workflow_type")
    private String workflowType;

    @TableField("lifecycle_status")
    private String lifecycleStatus;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("retired_at")
    private LocalDateTime retiredAt;

    @TableField("supersedes_id")
    private Long supersedesId;

    @TableField("revision")
    private Integer revision;
}
