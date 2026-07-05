package com.smartoa.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetPriceUpdate {
    private String symbol;
    private BigDecimal currentPrice;
}
