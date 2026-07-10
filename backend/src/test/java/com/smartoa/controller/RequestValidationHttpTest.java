package com.smartoa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoa.common.GlobalExceptionHandler;
import com.smartoa.dto.*;
import com.smartoa.entity.Portfolio;
import com.smartoa.entity.PortfolioAsset;
import com.smartoa.entity.PortfolioDividend;
import com.smartoa.entity.PortfolioTrade;
import com.smartoa.entity.User;
import com.smartoa.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("写请求 Bean Validation HTTP 测试（MockMvc standalone）")
class RequestValidationHttpTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LeaveService leaveService;
    private ExpenseService expenseService;
    private AccountingService accountingService;
    private PortfolioService portfolioService;
    private UserService userService;
    private ApprovalAuthorizationService authService;
    private MockMvc leaveMvc;
    private MockMvc expenseMvc;
    private MockMvc portfolioMvc;

    private User manager() {
        User u = new User();
        u.setId(1L);
        u.setRole("MANAGER");
        return u;
    }

    @BeforeEach
    void setUp() {
        leaveService = mock(LeaveService.class);
        expenseService = mock(ExpenseService.class);
        accountingService = mock(AccountingService.class);
        portfolioService = mock(PortfolioService.class);
        userService = mock(UserService.class);
        authService = mock(ApprovalAuthorizationService.class);

        when(userService.getLoginUser()).thenReturn(manager());

        leaveMvc = MockMvcBuilders.standaloneSetup(
                new LeaveController(leaveService, userService, authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        expenseMvc = MockMvcBuilders.standaloneSetup(
                new ExpenseController(expenseService, accountingService, userService, authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        portfolioMvc = MockMvcBuilders.standaloneSetup(
                new PortfolioController(portfolioService, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ======================== 请假提交 ========================

    @Nested
    @DisplayName("POST /api/leave/submit")
    class LeaveSubmit {

        @Test
        @DisplayName("templateId 缺失 → 400")
        void missingTemplateId() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"leaveType\":\"年假\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).submitLeave(any(), any());
        }

        @Test
        @DisplayName("leaveType 空白 → 400")
        void blankLeaveType() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":1,\"leaveType\":\" \",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).submitLeave(any(), any());
        }

        @Test
        @DisplayName("startDate 缺失 → 400")
        void missingStartDate() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":1,\"leaveType\":\"年假\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).submitLeave(any(), any());
        }

        @Test
        @DisplayName("endDate 早于 startDate → 400")
        void endDateBeforeStartDate() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":1,\"leaveType\":\"年假\",\"startDate\":\"2026-07-05\",\"endDate\":\"2026-07-01\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("结束日期不能早于开始日期"));
            verify(leaveService, never()).submitLeave(any(), any());
        }

        @Test
        @DisplayName("reason 空白 → 400")
        void blankReason() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":1,\"leaveType\":\"年假\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\",\"reason\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).submitLeave(any(), any());
        }

        @Test
        @DisplayName("reason 超长 → 400")
        void reasonTooLong() throws Exception {
            String longReason = "x".repeat(501);
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MAPPER.writeValueAsString(new java.util.HashMap<>() {{
                        put("templateId", 1);
                        put("leaveType", "年假");
                        put("startDate", "2026-07-01");
                        put("endDate", "2026-07-02");
                        put("reason", longReason);
                    }})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).submitLeave(any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200，调用 leaveService.submitLeave")
        void validRequest() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":1,\"leaveType\":\"年假\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(leaveService).submitLeave(eq(1L), any(LeaveSubmitRequest.class));
        }

        @Test
        @DisplayName("templateId=0 → 400，LeaveService 无交互")
        void templateIdZero() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":0,\"leaveType\":\"年假\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("模板ID必须为正整数"));
            verify(leaveService, never()).submitLeave(any(), any());
        }
    }

    // ======================== 请假审批 ========================

    @Nested
    @DisplayName("POST /api/leave/approve")
    class LeaveApprove {

        @Test
        @DisplayName("requestId 缺失 → 400")
        void missingRequestId() throws Exception {
            leaveMvc.perform(post("/api/leave/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"action\":\"APPROVE\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).approveLeave(any(), any(), any(), any());
        }

        @Test
        @DisplayName("action 非 APPROVE/REJECT → 400")
        void invalidAction() throws Exception {
            leaveMvc.perform(post("/api/leave/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"requestId\":1,\"action\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).approveLeave(any(), any(), any(), any());
        }

        @Test
        @DisplayName("comment 超长 → 400")
        void commentTooLong() throws Exception {
            leaveMvc.perform(post("/api/leave/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MAPPER.writeValueAsString(new java.util.HashMap<>() {{
                        put("requestId", 1);
                        put("action", "APPROVE");
                        put("comment", "x".repeat(501));
                    }})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).approveLeave(any(), any(), any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            leaveMvc.perform(post("/api/leave/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"requestId\":1,\"action\":\"APPROVE\",\"comment\":\"同意\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(leaveService).approveLeave(1L, 1L, "APPROVE", "同意");
        }
    }

    // ======================== 请假转派 ========================

    @Nested
    @DisplayName("POST /api/leave/{id}/transfer")
    class LeaveTransfer {

        @Test
        @DisplayName("toUserId 缺失 → 400")
        void missingToUserId() throws Exception {
            leaveMvc.perform(post("/api/leave/1/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(leaveService, never()).transferLeave(any(), any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            leaveMvc.perform(post("/api/leave/1/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"toUserId\":2}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(leaveService).transferLeave(1L, 1L, 2L);
        }

        @Test
        @DisplayName("toUserId=0 → 400，LeaveService 无交互")
        void toUserIdZero() throws Exception {
            leaveMvc.perform(post("/api/leave/1/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"toUserId\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("转派目标用户ID必须为正整数"));
            verify(leaveService, never()).transferLeave(any(), any(), any());
        }
    }

    // ======================== 经费提交 ========================

    @Nested
    @DisplayName("POST /api/expense/submit")
    class ExpenseSubmit {

        @Test
        @DisplayName("category 空白 → 400")
        void blankCategory() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\" \",\"amount\":100.00}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("amount 缺失 → 400")
        void missingAmount() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("amount=0 → 400")
        void zeroAmount() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("amount 为负 → 400")
        void negativeAmount() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":-10.00}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("amount 超过2位小数 → 400")
        void tooManyDecimalPlaces() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":10.123}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("description 超长 → 400")
        void descriptionTooLong() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MAPPER.writeValueAsString(new java.util.HashMap<>() {{
                        put("category", "办公");
                        put("amount", 100.00);
                        put("description", "x".repeat(501));
                    }})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("receiptUrl 超过500字 → 400")
        void receiptUrlTooLong() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MAPPER.writeValueAsString(new java.util.HashMap<>() {{
                        put("category", "办公");
                        put("amount", 100.00);
                        put("receiptUrl", "http://example.com/" + "x".repeat(482));
                    }})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("非HTTP协议URL ftp:// → 400，Service 无交互")
        void nonHttpUrlRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"ftp://files.example.com/receipt.pdf\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("javascript: URL → 400，Service 无交互")
        void javascriptUrlRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"javascript:alert(1)\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("data: URL → 400，Service 无交互")
        void dataUrlRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"data:text/plain,test\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("file: URL → 400，Service 无交互")
        void fileUrlRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"file:///tmp/a.pdf\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("protocol-relative URL → 400，Service 无交互")
        void protocolRelativeRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"//evil.example.com/a.pdf\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("相对路径 → 400，Service 无交互")
        void relativePathRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"/uploads/a.pdf\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("userInfo URL → 400，Service 无交互")
        void userInfoRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"https://user:pass@example.com/a.pdf\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("HTTPS 合法 URL → 200，Service 收到原始值")
        void httpsUrlAccepted() throws Exception {
            String url = "https://example.com/receipt.pdf";
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"" + url + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(expenseService).submitExpense(eq(1L), argThat(dto -> url.equals(dto.getReceiptUrl())));
        }

        @Test
        @DisplayName("HTTP localhost 合法 URL → 200，Service 收到原始值")
        void httpLocalhostAccepted() throws Exception {
            String url = "http://localhost:3000/uploads/receipt.pdf";
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"" + url + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(expenseService).submitExpense(eq(1L), argThat(dto -> url.equals(dto.getReceiptUrl())));
        }

        @Test
        @DisplayName("receiptUrl 缺失 → 合法 200")
        void missingReceiptUrlAccepted() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(expenseService).submitExpense(eq(1L), any(ExpenseSubmitRequest.class));
        }

        @Test
        @DisplayName("receiptUrl 空字符串 → 合法 200")
        void emptyReceiptUrlAccepted() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(expenseService).submitExpense(eq(1L), any(ExpenseSubmitRequest.class));
        }

        @Test
        @DisplayName("receiptUrl 纯空格 → 400")
        void onlySpacesReceiptUrlRejected() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00,\"receiptUrl\":\"   \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("附件URL仅允许有效的HTTP或HTTPS地址"));
            verify(expenseService, never()).submitExpense(any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            expenseMvc.perform(post("/api/expense/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"办公\",\"amount\":100.00}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(expenseService).submitExpense(eq(1L), any(ExpenseSubmitRequest.class));
        }
    }

    // ======================== 经费审批 ========================

    @Nested
    @DisplayName("POST /api/expense/approve")
    class ExpenseApprove {

        @Test
        @DisplayName("requestId 非正数 → 400")
        void invalidRequestId() throws Exception {
            expenseMvc.perform(post("/api/expense/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"requestId\":0,\"action\":\"APPROVE\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).approveExpense(any(), any(), any(), any());
        }

        @Test
        @DisplayName("action 非法 → 400")
        void invalidAction() throws Exception {
            expenseMvc.perform(post("/api/expense/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"requestId\":1,\"action\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).approveExpense(any(), any(), any(), any());
        }

        @Test
        @DisplayName("comment 超长 → 400")
        void commentTooLong() throws Exception {
            expenseMvc.perform(post("/api/expense/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MAPPER.writeValueAsString(new java.util.HashMap<>() {{
                        put("requestId", 1);
                        put("action", "APPROVE");
                        put("comment", "x".repeat(501));
                    }})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).approveExpense(any(), any(), any(), any());
        }
    }

    // ======================== 经费冲销 ========================

    @Nested
    @DisplayName("POST /api/expense/{id}/reverse")
    class ExpenseReverse {

        @Test
        @DisplayName("reason 空白 → 400")
        void blankReason() throws Exception {
            expenseMvc.perform(post("/api/expense/1/reverse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).reverseExpense(any(), any(), any());
        }

        @Test
        @DisplayName("reason 超长 → 400")
        void reasonTooLong() throws Exception {
            expenseMvc.perform(post("/api/expense/1/reverse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MAPPER.writeValueAsString(new java.util.HashMap<>() {{
                        put("reason", "x".repeat(501));
                    }})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(expenseService, never()).reverseExpense(any(), any(), any());
        }
    }

    // ======================== Portfolio 组合 ========================

    @Nested
    @DisplayName("POST /api/portfolio/portfolios")
    class PortfolioCreate {

        @Test
        @DisplayName("name 空白 → 400")
        void blankName() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).createPortfolio(any(), any(), any());
        }

        @Test
        @DisplayName("非法 baseCurrency → 400")
        void invalidBaseCurrency() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"baseCurrency\":\"us\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).createPortfolio(any(), any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            when(portfolioService.createPortfolio(any(), any(), any())).thenReturn(new Portfolio());
            portfolioMvc.perform(post("/api/portfolio/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"description\":\"Desc\",\"baseCurrency\":\"USD\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(portfolioService).createPortfolio("Test", "Desc", "USD");
        }
    }

    // ======================== Portfolio 资产标的 ========================

    @Nested
    @DisplayName("POST /api/portfolio/assets")
    class AssetCreate {

        @Test
        @DisplayName("symbol 空白 → 400")
        void blankSymbol() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"symbol\":\"\",\"name\":\"Apple\",\"assetType\":\"STOCK\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).createAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("name 空白 → 400")
        void blankName() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"symbol\":\"AAPL\",\"name\":\"\",\"assetType\":\"STOCK\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).createAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("非法 assetType → 400")
        void invalidAssetType() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"symbol\":\"AAPL\",\"name\":\"Apple\",\"assetType\":\"CRYPTO\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).createAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("非法 currency → 400")
        void invalidCurrency() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"symbol\":\"AAPL\",\"name\":\"Apple\",\"assetType\":\"STOCK\",\"currency\":\"us\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).createAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            when(portfolioService.createAsset(any(), any(), any(), any())).thenReturn(new PortfolioAsset());
            portfolioMvc.perform(post("/api/portfolio/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"symbol\":\"AAPL\",\"name\":\"Apple\",\"assetType\":\"STOCK\",\"currency\":\"USD\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(portfolioService).createAsset("AAPL", "Apple", "STOCK", "USD");
        }
    }

    // ======================== 资产价格 ========================

    @Nested
    @DisplayName("PUT /api/portfolio/assets/{id}/price")
    class AssetPrice {

        @Test
        @DisplayName("价格缺失 → 400")
        void missingPrice() throws Exception {
            portfolioMvc.perform(put("/api/portfolio/assets/1/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).updateAssetPrice(any(), any());
        }

        @Test
        @DisplayName("价格为零 → 400")
        void zeroPrice() throws Exception {
            portfolioMvc.perform(put("/api/portfolio/assets/1/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPrice\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).updateAssetPrice(any(), any());
        }

        @Test
        @DisplayName("价格为负 → 400")
        void negativePrice() throws Exception {
            portfolioMvc.perform(put("/api/portfolio/assets/1/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPrice\":-1.0000}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).updateAssetPrice(any(), any());
        }

        @Test
        @DisplayName("超过4位小数 → 400")
        void tooManyDecimals() throws Exception {
            portfolioMvc.perform(put("/api/portfolio/assets/1/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPrice\":100.12345}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).updateAssetPrice(any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200，使用 id 而非 symbol")
        void validRequestUsesId() throws Exception {
            portfolioMvc.perform(put("/api/portfolio/assets/42/price")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPrice\":100.1234}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(portfolioService).updateAssetPrice(42L, new java.math.BigDecimal("100.1234"));
        }
    }

    // ======================== 交易 ========================

    @Nested
    @DisplayName("POST /api/portfolio/trades")
    class Trade {

        @Test
        @DisplayName("tradeType 非法 → 400")
        void invalidTradeType() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"tradeType\":\"HOLD\",\"quantity\":10,\"price\":100}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).executeTrade(any(), any());
        }

        @Test
        @DisplayName("quantity 非正数 → 400")
        void nonPositiveQuantity() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"tradeType\":\"BUY\",\"quantity\":0,\"price\":100}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).executeTrade(any(), any());
        }

        @Test
        @DisplayName("price 非正数 → 400")
        void nonPositivePrice() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"tradeType\":\"BUY\",\"quantity\":10,\"price\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).executeTrade(any(), any());
        }

        @Test
        @DisplayName("fee 为负 → 400")
        void negativeFee() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"tradeType\":\"BUY\",\"quantity\":10,\"price\":100,\"fee\":-1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).executeTrade(any(), any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            when(portfolioService.executeTrade(any(), eq(1L))).thenReturn(new PortfolioTrade());
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"tradeType\":\"BUY\",\"quantity\":10,\"price\":100}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(portfolioService).executeTrade(any(TradeRequest.class), eq(1L));
        }

        @Test
        @DisplayName("portfolioId=0 → 400，PortfolioService 无交互")
        void portfolioIdZero() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":0,\"assetId\":1,\"tradeType\":\"BUY\",\"quantity\":10,\"price\":100}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("组合ID必须为正整数"));
            verify(portfolioService, never()).executeTrade(any(), any());
        }

        @Test
        @DisplayName("assetId=-1 → 400，PortfolioService 无交互")
        void assetIdNegative() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":-1,\"tradeType\":\"BUY\",\"quantity\":10,\"price\":100}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("资产标的ID必须为正整数"));
            verify(portfolioService, never()).executeTrade(any(), any());
        }
    }

    // ======================== 股息 ========================

    @Nested
    @DisplayName("POST /api/portfolio/dividends")
    class Dividend {

        @Test
        @DisplayName("amount 非正数 → 400")
        void nonPositiveAmount() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"amount\":0,\"perShare\":1.0,\"quantity\":10,\"dividendDate\":\"2026-06-30\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).recordDividend(any());
        }

        @Test
        @DisplayName("perShare 非正数 → 400")
        void nonPositivePerShare() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"amount\":100,\"perShare\":0,\"quantity\":10,\"dividendDate\":\"2026-06-30\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).recordDividend(any());
        }

        @Test
        @DisplayName("quantity 非正数 → 400")
        void nonPositiveQuantity() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"amount\":100,\"perShare\":1.0,\"quantity\":0,\"dividendDate\":\"2026-06-30\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).recordDividend(any());
        }

        @Test
        @DisplayName("dividendDate 缺失 → 400")
        void missingDividendDate() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"amount\":100,\"perShare\":1.0,\"quantity\":10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            verify(portfolioService, never()).recordDividend(any());
        }

        @Test
        @DisplayName("合法请求 → 200")
        void validRequest() throws Exception {
            when(portfolioService.recordDividend(any())).thenReturn(new PortfolioDividend());
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":1,\"amount\":100,\"perShare\":1.0,\"quantity\":10,\"dividendDate\":\"2026-06-30\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            verify(portfolioService).recordDividend(any(DividendRequest.class));
        }

        @Test
        @DisplayName("portfolioId=0 → 400，PortfolioService 无交互")
        void portfolioIdZero() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":0,\"assetId\":1,\"amount\":100,\"perShare\":1.0,\"quantity\":10,\"dividendDate\":\"2026-06-30\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("组合ID必须为正整数"));
            verify(portfolioService, never()).recordDividend(any());
        }

        @Test
        @DisplayName("assetId=-1 → 400，PortfolioService 无交互")
        void assetIdNegative() throws Exception {
            portfolioMvc.perform(post("/api/portfolio/dividends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"portfolioId\":1,\"assetId\":-1,\"amount\":100,\"perShare\":1.0,\"quantity\":10,\"dividendDate\":\"2026-06-30\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("资产标的ID必须为正整数"));
            verify(portfolioService, never()).recordDividend(any());
        }
    }

    // ======================== JSON 格式错误 ========================

    @Nested
    @DisplayName("JSON 格式与类型错误")
    class MalformedJson {

        @Test
        @DisplayName("非法 JSON → 400 / code 400 / 请求参数格式错误")
        void invalidJson() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("请求参数格式错误"));
        }

        @Test
        @DisplayName("日期格式错误 → 400")
        void invalidDateFormat() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":1,\"leaveType\":\"年假\",\"startDate\":\"not-a-date\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("数字字段传字符串 → 400")
        void stringForNumberField() throws Exception {
            leaveMvc.perform(post("/api/leave/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"templateId\":\"abc\",\"leaveType\":\"年假\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\",\"reason\":\"休息\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }
}
