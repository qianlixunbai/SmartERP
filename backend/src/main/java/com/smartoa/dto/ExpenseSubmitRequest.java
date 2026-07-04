package com.smartoa.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseSubmitRequest {
    private Long templateId;
    private String category;
    private BigDecimal amount;
    private String description;
    private String receiptUrl;
}
