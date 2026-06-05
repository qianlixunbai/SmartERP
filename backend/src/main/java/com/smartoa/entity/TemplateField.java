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
@TableName("template_field")
public class TemplateField {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("field_name")
    private String fieldName;

    @TableField("field_label")
    private String fieldLabel;

    @TableField("field_type")
    private String fieldType;

    @TableField("required")
    private Boolean required;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("options")
    private String options;

    @TableField("create_time")
    private LocalDateTime createTime;
}
