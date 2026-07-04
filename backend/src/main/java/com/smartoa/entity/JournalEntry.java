package com.smartoa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("journal_entry")
public class JournalEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("account_id")
    private Long accountId;

    @TableField("debit")
    private BigDecimal debit;

    @TableField("credit")
    private BigDecimal credit;

    @TableField("memo")
    private String memo;

    @TableField("created_by")
    private Long createdBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @Version
    @TableField("version")
    private Integer version;
}
