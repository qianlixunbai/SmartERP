package com.smartoa.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TradeRequest {
    @NotNull(message = "组合ID不能为空")
    private Long portfolioId;

    @NotNull(message = "资产标的ID不能为空")
    private Long assetId;

    @NotBlank(message = "交易类型不能为空")
    @Pattern(regexp = "BUY|SELL", message = "交易类型只能为BUY或SELL")
    private String tradeType;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.000001", message = "数量必须大于0")
    @Digits(integer = 13, fraction = 6, message = "数量最多6位小数")
    private BigDecimal quantity;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0001", message = "价格必须大于0")
    @Digits(integer = 15, fraction = 4, message = "价格最多4位小数")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "手续费不能为负数")
    @Digits(integer = 17, fraction = 2, message = "手续费最多2位小数")
    private BigDecimal fee;

    private LocalDate tradeDate;

    @Size(max = 300, message = "备注不能超过300字")
    private String memo;
}
