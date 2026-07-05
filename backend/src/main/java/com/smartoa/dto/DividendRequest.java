package com.smartoa.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DividendRequest {
    private Long portfolioId;
    private Long assetId;
    private BigDecimal amount;
    private BigDecimal perShare;
    private BigDecimal quantity;
    private LocalDate dividendDate;
}
