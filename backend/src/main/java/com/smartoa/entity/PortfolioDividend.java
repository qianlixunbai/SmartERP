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
@TableName("portfolio_dividend")
public class PortfolioDividend {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("portfolio_id")
    private Long portfolioId;

    @TableField("asset_id")
    private Long assetId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("per_share")
    private BigDecimal perShare;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("dividend_date")
    private LocalDate dividendDate;

    @TableField("create_time")
    private LocalDateTime createTime;
}
