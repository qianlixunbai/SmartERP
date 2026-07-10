package com.smartoa.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseSubmitRequest {
    @Positive(message = "模板ID必须为正整数")
    private Long templateId;

    @NotBlank(message = "费用类别不能为空")
    @Size(max = 50, message = "费用类别不能超过50字")
    private String category;

    @Positive(message = "成本中心ID必须为正整数")
    private Long costCenterId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @Digits(integer = 17, fraction = 2, message = "金额最多2位小数")
    private BigDecimal amount;

    @Size(max = 500, message = "费用说明不能超过500字")
    private String description;

    @Size(max = 500, message = "附件URL不能超过500字")
    private String receiptUrl;
}
