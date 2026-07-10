package com.smartoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortfolioAssetCreateRequest {
    @NotBlank(message = "标的代码不能为空")
    @Size(max = 20, message = "标的代码不能超过20字")
    private String symbol;

    @NotBlank(message = "标的名称不能为空")
    @Size(max = 200, message = "标的名称不能超过200字")
    private String name;

    @NotBlank(message = "资产类型不能为空")
    @Pattern(regexp = "ETF|STOCK|BOND|CASH", message = "资产类型只能为ETF、STOCK、BOND或CASH")
    private String assetType;

    @Pattern(regexp = "[A-Z]{3}", message = "币种必须为3位大写字母")
    private String currency;
}
