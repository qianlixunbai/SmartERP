package com.smartoa.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TradeRequest {
    private Long portfolioId;
    private Long assetId;
    private String tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal fee;
    private LocalDate tradeDate;
    private String memo;
}
