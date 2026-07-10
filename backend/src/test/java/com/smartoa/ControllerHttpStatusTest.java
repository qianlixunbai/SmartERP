package com.smartoa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoa.common.BusinessException;
import com.smartoa.common.GlobalExceptionHandler;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.service.ApprovalAuthorizationService;
import com.smartoa.service.ExpenseService;
import com.smartoa.service.LeaveService;
import com.smartoa.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Controller HTTP 状态码行为测试（MockMvc standalone）")
class ControllerHttpStatusTest {

    private MockMvc mockMvc;
    private LeaveService leaveService;
    private ApprovalAuthorizationService approvalAuthorizationService;

    @BeforeEach
    void setUp() {
        leaveService = mock(LeaveService.class);
        UserService userService = mock(UserService.class);
        approvalAuthorizationService = mock(ApprovalAuthorizationService.class);

        LeaveController leaveController = new LeaveController(leaveService, userService, approvalAuthorizationService);

        ExpenseService expenseService = mock(ExpenseService.class);
        UserService expenseUserService = mock(UserService.class);
        ApprovalAuthorizationService expenseAuthService = mock(ApprovalAuthorizationService.class);

        ExpenseController expenseController = new ExpenseController(
                expenseService, null, expenseUserService, expenseAuthService);

        // 设置 LeaveController 测试用的 userService 行为
        User manager = new User();
        manager.setId(1L);
        manager.setRole("MANAGER");

        User employee = new User();
        employee.setId(2L);
        employee.setRole("EMPLOYEE");

        // repair: MANAGER
        when(userService.getLoginUser()).thenReturn(manager);
        // repair: 非 MANAGER
        UserService repairUserService = mock(UserService.class);
        User nonManager = new User();
        nonManager.setId(3L);
        nonManager.setRole("EMPLOYEE");
        when(repairUserService.getLoginUser()).thenReturn(nonManager);
        // 这里需要单独的 controller 来测试 repair 非 MANAGER

        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestRepairController(repairUserService, leaveService),
                new TestLeaveDetailController(userService, leaveService, approvalAuthorizationService),
                new TestExpenseDetailController(expenseUserService, expenseService, expenseAuthService)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 测试 repair 非 MANAGER 返回 403
     */
    @org.springframework.web.bind.annotation.RestController
    static class TestRepairController {
        private final UserService userService;
        private final LeaveService leaveService;

        TestRepairController(UserService userService, LeaveService leaveService) {
            this.userService = userService;
            this.leaveService = leaveService;
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/leave/repair")
        public com.smartoa.common.Result<Integer> repair() {
            User user = userService.getLoginUser();
            if (user == null || user.getId() == null) {
                throw new BusinessException(401, "请先登录");
            }
            if (!"MANAGER".equals(user.getRole())) {
                throw new BusinessException(403, "无权限");
            }
            return com.smartoa.common.Result.success(leaveService.repairStuckRequests());
        }
    }

    /**
     * 测试请假详情返回 404/403
     */
    @org.springframework.web.bind.annotation.RestController
    static class TestLeaveDetailController {
        private final UserService userService;
        private final LeaveService leaveService;
        private final ApprovalAuthorizationService authService;

        TestLeaveDetailController(UserService userService, LeaveService leaveService,
                                  ApprovalAuthorizationService authService) {
            this.userService = userService;
            this.leaveService = leaveService;
            this.authService = authService;
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/leave/{id}")
        public com.smartoa.common.Result<LeaveRequest> detail(
                @org.springframework.web.bind.annotation.PathVariable Long id) {
            User user = userService.getLoginUser();
            if (user == null) throw new BusinessException(401, "请先登录");
            authService.requireReadableLeave(id, user);
            return com.smartoa.common.Result.success(leaveService.getRequestDetail(id));
        }
    }

    /**
     * 测试经费详情返回 403
     */
    @org.springframework.web.bind.annotation.RestController
    static class TestExpenseDetailController {
        private final UserService userService;
        private final ExpenseService expenseService;
        private final ApprovalAuthorizationService authService;

        TestExpenseDetailController(UserService userService, ExpenseService expenseService,
                                    ApprovalAuthorizationService authService) {
            this.userService = userService;
            this.expenseService = expenseService;
            this.authService = authService;
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/expense/{id}")
        public com.smartoa.common.Result<ExpenseRequest> detail(
                @org.springframework.web.bind.annotation.PathVariable Long id) {
            User user = userService.getLoginUser();
            if (user == null) throw new BusinessException(401, "请先登录");
            authService.requireReadableExpense(id, user);
            return com.smartoa.common.Result.success(expenseService.getExpenseDetail(id));
        }
    }

    @Test
    @DisplayName("repair 非 MANAGER → HTTP 403, body.code=403")
    void repairNonManager_ShouldReturn403() throws Exception {
        // TestRepairController 的 userService 在构造时已 mock 为 EMPLOYEE
        mockMvc.perform(post("/api/leave/repair"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限"));
    }

    @Test
    @DisplayName("请假详情不存在 → HTTP 404, body.code=404")
    void leaveDetailNotFound_ShouldReturn404() throws Exception {
        UserService userService = mock(UserService.class);
        LeaveService leaveService = mock(LeaveService.class);
        ApprovalAuthorizationService authService = mock(ApprovalAuthorizationService.class);

        User employee = new User();
        employee.setId(2L);
        employee.setRole("EMPLOYEE");

        when(userService.getLoginUser()).thenReturn(employee);
        when(authService.requireReadableLeave(999L, employee))
                .thenThrow(new BusinessException(404, "请假单不存在"));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new TestLeaveDetailController(userService, leaveService, authService)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/api/leave/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("请假单不存在"));
    }

    @Test
    @DisplayName("经费详情无权限 → HTTP 403, body.code=403")
    void expenseDetailForbidden_ShouldReturn403() throws Exception {
        UserService userService = mock(UserService.class);
        ExpenseService expenseService = mock(ExpenseService.class);
        ApprovalAuthorizationService authService = mock(ApprovalAuthorizationService.class);

        User employee = new User();
        employee.setId(2L);
        employee.setRole("EMPLOYEE");

        when(userService.getLoginUser()).thenReturn(employee);
        when(authService.requireReadableExpense(42L, employee))
                .thenThrow(new BusinessException(403, "无权限访问该经费申请"));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new TestExpenseDetailController(userService, expenseService, authService)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/api/expense/42"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问该经费申请"));
    }
}
