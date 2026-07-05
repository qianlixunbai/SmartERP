package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("portfolio_holding")
public class PortfolioHolding {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("portfolio_id")
    private Long portfolioId;

    @TableField("asset_id")
    private Long assetId;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("avg_cost")
    private BigDecimal avgCost;

    @TableField("total_cost")
    private BigDecimal totalCost;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
