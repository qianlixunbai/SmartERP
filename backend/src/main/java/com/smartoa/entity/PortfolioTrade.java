package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("portfolio_trade")
public class PortfolioTrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("portfolio_id")
    private Long portfolioId;

    @TableField("asset_id")
    private Long assetId;

    @TableField("trade_type")
    private String tradeType;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("price")
    private BigDecimal price;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("fee")
    private BigDecimal fee;

    @TableField("trade_date")
    private LocalDate tradeDate;

    @TableField("memo")
    private String memo;

    @TableField("create_time")
    private LocalDateTime createTime;
}
