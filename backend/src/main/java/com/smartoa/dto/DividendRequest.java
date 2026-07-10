package com.smartoa.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DividendRequest {
    @NotNull(message = "组合ID不能为空")
    @Positive(message = "组合ID必须为正整数")
    private Long portfolioId;

    @NotNull(message = "资产标的ID不能为空")
    @Positive(message = "资产标的ID必须为正整数")
    private Long assetId;

    @NotNull(message = "股息金额不能为空")
    @DecimalMin(value = "0.01", message = "股息金额必须大于0")
    @Digits(integer = 17, fraction = 2, message = "股息金额最多2位小数")
    private BigDecimal amount;

    @NotNull(message = "每股股息不能为空")
    @DecimalMin(value = "0.0001", message = "每股股息必须大于0")
    @Digits(integer = 15, fraction = 4, message = "每股股息最多4位小数")
    private BigDecimal perShare;

    @NotNull(message = "持有数量不能为空")
    @DecimalMin(value = "0.000001", message = "持有数量必须大于0")
    @Digits(integer = 13, fraction = 6, message = "持有数量最多6位小数")
    private BigDecimal quantity;

    @NotNull(message = "派息日不能为空")
    private LocalDate dividendDate;
}
