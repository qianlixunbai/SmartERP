package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.GlobalExceptionHandler;
import com.smartoa.entity.User;
import com.smartoa.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Controller HTTP 状态码行为测试（真实 Controller，MockMvc standalone）")
class ControllerHttpStatusTest {

    @Test
    @DisplayName("repair 非 MANAGER → HTTP 403, body.code=403, repair 未调用")
    void repairNonManager_ShouldReturn403() throws Exception {
        LeaveService leaveService = mock(LeaveService.class);
        UserService userService = mock(UserService.class);
        ApprovalAuthorizationService authService = mock(ApprovalAuthorizationService.class);

        User employee = new User();
        employee.setId(3L);
        employee.setRole("EMPLOYEE");
        when(userService.getLoginUser()).thenReturn(employee);

        LeaveController controller = new LeaveController(leaveService, userService, authService);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/leave/repair"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限"));

        verify(leaveService, never()).repairStuckRequests();
    }

    @Test
    @DisplayName("请假详情不存在 → HTTP 404, body.code=404, 详情 Service 未调用")
    void leaveDetailNotFound_ShouldReturn404() throws Exception {
        LeaveService leaveService = mock(LeaveService.class);
        UserService userService = mock(UserService.class);
        ApprovalAuthorizationService authService = mock(ApprovalAuthorizationService.class);

        User employee = new User();
        employee.setId(2L);
        employee.setRole("EMPLOYEE");
        when(userService.getLoginUser()).thenReturn(employee);
        when(authService.requireReadableLeave(999L, employee))
                .thenThrow(new BusinessException(404, "请假单不存在"));

        LeaveController controller = new LeaveController(leaveService, userService, authService);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/leave/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("请假单不存在"));

        verify(leaveService, never()).getRequestDetail(anyLong());
    }

    @Test
    @DisplayName("经费详情无权限 → HTTP 403, body.code=403, 详情 Service 未调用")
    void expenseDetailForbidden_ShouldReturn403() throws Exception {
        ExpenseService expenseService = mock(ExpenseService.class);
        AccountingService accountingService = mock(AccountingService.class);
        UserService userService = mock(UserService.class);
        ApprovalAuthorizationService authService = mock(ApprovalAuthorizationService.class);

        User employee = new User();
        employee.setId(2L);
        employee.setRole("EMPLOYEE");
        when(userService.getLoginUser()).thenReturn(employee);
        when(authService.requireReadableExpense(42L, employee))
                .thenThrow(new BusinessException(403, "无权限访问该经费申请"));

        ExpenseController controller = new ExpenseController(
                expenseService, accountingService, userService, authService);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/expense/42"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问该经费申请"));

        verify(expenseService, never()).getExpenseDetail(anyLong());
    }
}
