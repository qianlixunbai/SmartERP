package com.smartoa.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Bean Validation 单元测试（不启动 Spring Context）")
class RequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private <T> void assertHasViolation(T dto, String expectedMessagePart) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains(expectedMessagePart)),
                "期望包含 '" + expectedMessagePart + "' 的校验失败，实际: " + violations);
    }

    private <T> void assertNoViolation(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "期望无校验失败，实际: " + violations);
    }

    // ======================== LeaveSubmitRequest ========================

    @Test
    @DisplayName("请假：合法 DTO 无 violation")
    void leaveSubmit_valid_noViolation() {
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.of(2026, 7, 1));
        dto.setEndDate(LocalDate.of(2026, 7, 2));
        dto.setReason("休息");
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("请假：同一天允许")
    void leaveSubmit_sameDay_valid() {
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.of(2026, 7, 1));
        dto.setEndDate(LocalDate.of(2026, 7, 1));
        dto.setReason("休息");
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("请假：endDate 早于 startDate")
    void leaveSubmit_endDateBeforeStartDate() {
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.of(2026, 7, 5));
        dto.setEndDate(LocalDate.of(2026, 7, 1));
        dto.setReason("休息");
        assertHasViolation(dto, "结束日期不能早于开始日期");
    }

    @Test
    @DisplayName("请假：startDate 为 null 时跨字段校验不误报")
    void leaveSubmit_nullStartDate_noDateViolation() {
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setEndDate(LocalDate.of(2026, 7, 2));
        dto.setReason("休息");
        Set<ConstraintViolation<LeaveSubmitRequest>> violations = validator.validate(dto);
        assertTrue(violations.stream()
                .noneMatch(v -> v.getMessage().contains("结束日期不能早于开始日期")));
    }

    // ======================== ExpenseSubmitRequest ========================

    @Test
    @DisplayName("经费：合法 DTO 无 violation")
    void expenseSubmit_valid_noViolation() {
        ExpenseSubmitRequest dto = new ExpenseSubmitRequest();
        dto.setCategory("办公");
        dto.setAmount(new BigDecimal("100.00"));
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("经费：金额超过2位小数")
    void expenseSubmit_tooManyDecimals() {
        ExpenseSubmitRequest dto = new ExpenseSubmitRequest();
        dto.setCategory("办公");
        dto.setAmount(new BigDecimal("10.123"));
        assertHasViolation(dto, "金额最多2位小数");
    }

    @Test
    @DisplayName("经费：可选字段为 null 不误报")
    void expenseSubmit_optionalFieldsNull() {
        ExpenseSubmitRequest dto = new ExpenseSubmitRequest();
        dto.setCategory("办公");
        dto.setAmount(new BigDecimal("100.00"));
        dto.setDescription(null);
        dto.setReceiptUrl(null);
        dto.setTemplateId(null);
        dto.setCostCenterId(null);
        assertNoViolation(dto);
    }

    // ======================== TradeRequest ========================

    @Test
    @DisplayName("交易：合法 DTO 无 violation")
    void tradeRequest_valid_noViolation() {
        TradeRequest dto = new TradeRequest();
        dto.setPortfolioId(1L);
        dto.setAssetId(1L);
        dto.setTradeType("BUY");
        dto.setQuantity(new BigDecimal("10.000000"));
        dto.setPrice(new BigDecimal("100.0000"));
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("交易：quantity 为0")
    void tradeRequest_zeroQuantity() {
        TradeRequest dto = new TradeRequest();
        dto.setPortfolioId(1L);
        dto.setAssetId(1L);
        dto.setTradeType("BUY");
        dto.setQuantity(BigDecimal.ZERO);
        dto.setPrice(new BigDecimal("100.0000"));
        assertHasViolation(dto, "数量必须大于0");
    }

    @Test
    @DisplayName("交易：price 为负")
    void tradeRequest_negativePrice() {
        TradeRequest dto = new TradeRequest();
        dto.setPortfolioId(1L);
        dto.setAssetId(1L);
        dto.setTradeType("BUY");
        dto.setQuantity(new BigDecimal("10"));
        dto.setPrice(new BigDecimal("-1.0000"));
        assertHasViolation(dto, "价格必须大于0");
    }

    // ======================== DividendRequest ========================

    @Test
    @DisplayName("股息：合法 DTO 无 violation")
    void dividendRequest_valid_noViolation() {
        DividendRequest dto = new DividendRequest();
        dto.setPortfolioId(1L);
        dto.setAssetId(1L);
        dto.setAmount(new BigDecimal("100.00"));
        dto.setPerShare(new BigDecimal("1.0000"));
        dto.setQuantity(new BigDecimal("10.000000"));
        dto.setDividendDate(LocalDate.of(2026, 6, 30));
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("股息：dividendDate 缺失")
    void dividendRequest_missingDate() {
        DividendRequest dto = new DividendRequest();
        dto.setPortfolioId(1L);
        dto.setAssetId(1L);
        dto.setAmount(new BigDecimal("100.00"));
        dto.setPerShare(new BigDecimal("1.0000"));
        dto.setQuantity(new BigDecimal("10.000000"));
        assertHasViolation(dto, "派息日不能为空");
    }

    // ======================== AssetPriceUpdate ========================

    @Test
    @DisplayName("价格：合法 DTO 无 violation")
    void assetPriceUpdate_valid_noViolation() {
        AssetPriceUpdate dto = new AssetPriceUpdate();
        dto.setCurrentPrice(new BigDecimal("100.1234"));
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("价格：超过4位小数")
    void assetPriceUpdate_tooManyDecimals() {
        AssetPriceUpdate dto = new AssetPriceUpdate();
        dto.setCurrentPrice(new BigDecimal("100.12345"));
        assertHasViolation(dto, "价格最多4位小数");
    }

    @Test
    @DisplayName("价格：为零")
    void assetPriceUpdate_zero() {
        AssetPriceUpdate dto = new AssetPriceUpdate();
        dto.setCurrentPrice(BigDecimal.ZERO);
        assertHasViolation(dto, "价格必须大于0");
    }

    // ======================== PortfolioCreateRequest ========================

    @Test
    @DisplayName("组合：合法 DTO 无 violation")
    void portfolioCreate_valid_noViolation() {
        PortfolioCreateRequest dto = new PortfolioCreateRequest();
        dto.setName("测试组合");
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("组合：非法 baseCurrency")
    void portfolioCreate_invalidCurrency() {
        PortfolioCreateRequest dto = new PortfolioCreateRequest();
        dto.setName("测试组合");
        dto.setBaseCurrency("usd");
        assertHasViolation(dto, "币种必须为3位大写字母");
    }

    // ======================== PortfolioAssetCreateRequest ========================

    @Test
    @DisplayName("资产：合法 DTO 无 violation")
    void assetCreate_valid_noViolation() {
        PortfolioAssetCreateRequest dto = new PortfolioAssetCreateRequest();
        dto.setSymbol("AAPL");
        dto.setName("Apple");
        dto.setAssetType("STOCK");
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("资产：非法 assetType")
    void assetCreate_invalidAssetType() {
        PortfolioAssetCreateRequest dto = new PortfolioAssetCreateRequest();
        dto.setSymbol("BTC");
        dto.setName("Bitcoin");
        dto.setAssetType("CRYPTO");
        assertHasViolation(dto, "资产类型只能为ETF、STOCK、BOND或CASH");
    }

    // ======================== LeaveApproveRequest ========================

    @Test
    @DisplayName("审批：合法 DTO 无 violation")
    void leaveApprove_valid_noViolation() {
        LeaveApproveRequest dto = new LeaveApproveRequest();
        dto.setRequestId(1L);
        dto.setAction("APPROVE");
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("审批：非法 action")
    void leaveApprove_invalidAction() {
        LeaveApproveRequest dto = new LeaveApproveRequest();
        dto.setRequestId(1L);
        dto.setAction("INVALID");
        assertHasViolation(dto, "审批动作只能为APPROVE或REJECT");
    }

    // ======================== ExpenseReverseRequest ========================

    @Test
    @DisplayName("冲销：合法 DTO 无 violation")
    void expenseReverse_valid_noViolation() {
        ExpenseReverseRequest dto = new ExpenseReverseRequest();
        dto.setReason("冲销原因");
        assertNoViolation(dto);
    }

    @Test
    @DisplayName("冲销：reason 为空白")
    void expenseReverse_blankReason() {
        ExpenseReverseRequest dto = new ExpenseReverseRequest();
        dto.setReason("  ");
        assertHasViolation(dto, "冲销原因不能为空");
    }
}
