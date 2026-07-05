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
@TableName("portfolio_asset")
public class PortfolioAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("symbol")
    private String symbol;

    @TableField("name")
    private String name;

    @TableField("asset_type")
    private String assetType;

    @TableField("currency")
    private String currency;

    @TableField("current_price")
    private BigDecimal currentPrice;

    @TableField("price_date")
    private LocalDate priceDate;

    @TableField("create_time")
    private LocalDateTime createTime;
}
