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
@TableName("period_balance")
public class PeriodBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("period_id")
    private Long periodId;

    @TableField("account_id")
    private Long accountId;

    @TableField("cost_center_id")
    private Long costCenterId;

    @TableField("profit_center_id")
    private Long profitCenterId;

    @TableField("debit_total")
    private BigDecimal debitTotal;

    @TableField("credit_total")
    private BigDecimal creditTotal;

    @TableField("closing_balance")
    private BigDecimal closingBalance;

    @TableField("create_time")
    private LocalDateTime createTime;
}
