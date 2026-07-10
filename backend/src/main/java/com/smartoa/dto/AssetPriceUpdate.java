package com.smartoa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetPriceUpdate {
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0001", message = "价格必须大于0")
    @Digits(integer = 15, fraction = 4, message = "价格最多4位小数")
    private BigDecimal currentPrice;
}
