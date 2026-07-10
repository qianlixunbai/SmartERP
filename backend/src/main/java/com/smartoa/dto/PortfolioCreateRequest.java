package com.smartoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortfolioCreateRequest {
    @NotBlank(message = "组合名称不能为空")
    @Size(max = 100, message = "组合名称不能超过100字")
    private String name;

    @Size(max = 300, message = "组合描述不能超过300字")
    private String description;

    @Pattern(regexp = "[A-Z]{3}", message = "币种必须为3位大写字母")
    private String baseCurrency;
}
