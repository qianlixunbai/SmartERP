package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
@TableName("fiscal_period")
public class FiscalPeriod {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("year")
    private Integer year;

    @TableField("month")
    private Integer month;

    @TableField("status")
    private String status;

    @TableField(value = "closed_by", updateStrategy = FieldStrategy.ALWAYS)
    private Long closedBy;

    @TableField(value = "closed_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime closedTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
