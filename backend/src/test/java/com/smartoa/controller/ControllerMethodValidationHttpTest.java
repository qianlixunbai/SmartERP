package com.smartoa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoa.common.GlobalExceptionHandler;
import com.smartoa.dto.AssetPriceUpdate;
import com.smartoa.dto.ExpenseReverseRequest;
import com.smartoa.dto.LeaveTransferRequest;
import com.smartoa.entity.User;
import com.smartoa.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 使用最小 Spring 容器 + MethodValidationPostProcessor 验证
 * Controller 上 @Validated + @Positive(PathVariable) 的方法级校验。
 *
 * <p>不加载完整 Spring Boot 上下文，不依赖数据库、MyBatis、JWT 或 Testcontainers。</p>
 */
@DisplayName("Controller 方法级校验 HTTP 测试（最小 Spring 容器代理）")
class ControllerMethodValidationHttpTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<GenericApplicationContext> contexts = new ArrayList<>();

    private LeaveService leaveService;
    private ExpenseService expenseService;
    private PortfolioService portfolioService;
    private UserService userService;
    private ApprovalAuthorizationService authService;
    private AccountingService accountingService;

    private MockMvc leaveMvc;
    private MockMvc expenseMvc;
    private MockMvc portfolioMvc;

    /**
     * 创建带有方法校验代理的 Controller 实例。
     *
     * <p>通过 GenericApplicationContext 注册 MethodValidationPostProcessor，
     * 让 Spring 为 @Validated 标注的 Controller 创建 CGLIB 代理，
     * 使 @Positive 等约束在方法调用时被实际执行。</p>
     */
    private <T> T createValidatedController(Class<T> type, Supplier<T> supplier) {
        GenericApplicationContext context = new GenericApplicationContext();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        context.getBeanFactory().registerSingleton("validator", validator);

        context.registerBean(MethodValidationPostProcessor.class, () -> {
            MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
            processor.setValidator(validator);
            processor.setProxyTargetClass(true);
            return processor;
        });

        context.registerBean(type, supplier);
        context.refresh();

        contexts.add(context);
        return context.getBean(type);
    }

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
        portfolioService = mock(PortfolioService.class);
        userService = mock(UserService.class);
        authService = mock(ApprovalAuthorizationService.class);
        accountingService = mock(AccountingService.class);

        when(userService.getLoginUser()).thenReturn(manager());

        LeaveController leaveProxy = createValidatedController(
                LeaveController.class,
                () -> new LeaveController(leaveService, userService, authService));
        leaveMvc = MockMvcBuilders
                .standaloneSetup(leaveProxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ExpenseController expenseProxy = createValidatedController(
                ExpenseController.class,
                () -> new ExpenseController(expenseService, accountingService, userService, authService));
        expenseMvc = MockMvcBuilders
                .standaloneSetup(expenseProxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        PortfolioController portfolioProxy = createValidatedController(
                PortfolioController.class,
                () -> new PortfolioController(portfolioService, userService));
        portfolioMvc = MockMvcBuilders
                .standaloneSetup(portfolioProxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        contexts.forEach(GenericApplicationContext::close);
        contexts.clear();
    }

    // ======================== 1. 请假撤回 ========================

    @Test
    @DisplayName("POST /api/leave/0/withdraw → 400, PathVariable @Positive 拦截 id=0")
    void leaveWithdraw_idZero_returns400() throws Exception {
        leaveMvc.perform(post("/api/leave/0/withdraw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ID必须为正整数"));

        verifyNoInteractions(leaveService);
    }

    // ======================== 2. 请假转派 ========================

    @Test
    @DisplayName("POST /api/leave/0/transfer → 400, PathVariable @Positive 拦截 id=0")
    void leaveTransfer_idZero_returns400() throws Exception {
        leaveMvc.perform(post("/api/leave/0/transfer")
                        .contentType("application/json")
                        .content(MAPPER.writeValueAsString(new LeaveTransferRequest() {{
                            setToUserId(2L);
                        }})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ID必须为正整数"));

        verifyNoInteractions(leaveService);
    }

    // ======================== 3. 经费撤回 ========================

    @Test
    @DisplayName("POST /api/expense/0/withdraw → 400, PathVariable @Positive 拦截 id=0")
    void expenseWithdraw_idZero_returns400() throws Exception {
        expenseMvc.perform(post("/api/expense/0/withdraw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ID必须为正整数"));

        verifyNoInteractions(expenseService);
    }

    // ======================== 4. 经费冲销 ========================

    @Test
    @DisplayName("POST /api/expense/0/reverse → 400, PathVariable @Positive 拦截 id=0")
    void expenseReverse_idZero_returns400() throws Exception {
        ExpenseReverseRequest body = new ExpenseReverseRequest();
        body.setReason("测试冲销");

        expenseMvc.perform(post("/api/expense/0/reverse")
                        .contentType("application/json")
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ID必须为正整数"));

        verifyNoInteractions(expenseService);
    }

    // ======================== 5. Portfolio 价格更新 ========================

    @Test
    @DisplayName("PUT /api/portfolio/assets/0/price → 400, PathVariable @Positive 拦截 id=0")
    void portfolioUpdatePrice_idZero_returns400() throws Exception {
        AssetPriceUpdate body = new AssetPriceUpdate();
        body.setCurrentPrice(new BigDecimal("100.1234"));

        portfolioMvc.perform(put("/api/portfolio/assets/0/price")
                        .contentType("application/json")
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ID必须为正整数"));

        verifyNoInteractions(portfolioService);
    }

    // ======================== 6. 验证实际异常类型 ========================

    @Test
    @DisplayName("验证 Spring 代理后 @Positive 校验抛出 ConstraintViolationException")
    void verifyActualExceptionType_isConstraintViolationException() {
        LeaveController proxy = createValidatedController(
                LeaveController.class,
                () -> new LeaveController(leaveService, userService, authService));

        // 直接调用代理方法，id=0 违反 @Positive
        assertThrows(ConstraintViolationException.class, () -> {
            proxy.withdrawLeave(0L);
        }, "Spring MethodValidationPostProcessor 代理应抛出 ConstraintViolationException");

        // 确认 Service 未被调用
        verifyNoInteractions(leaveService);
    }
}
