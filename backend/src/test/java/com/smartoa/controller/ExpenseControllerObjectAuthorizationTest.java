package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.*;
import com.smartoa.service.AccountingService;
import com.smartoa.service.ApprovalAuthorizationService;
import com.smartoa.service.ExpenseService;
import com.smartoa.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseController 对象级权限测试（Mockito）")
class ExpenseControllerObjectAuthorizationTest {

    private static final Long REQUEST_ID = 42L;
    private static final Long USER_ID = 1L;
    private static final Long UNRELATED_ID = 99L;

    @Mock
    private ExpenseService expenseService;
    @Mock
    private AccountingService accountingService;
    @Mock
    private UserService userService;
    @Mock
    private ApprovalAuthorizationService approvalAuthorizationService;

    private ExpenseController expenseController;

    @BeforeEach
    void setUp() {
        expenseController = new ExpenseController(
                expenseService, accountingService, userService, approvalAuthorizationService);
    }

    // ======================== 辅助方法 ========================

    private User user(Long id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    // ======================== 经费详情 ========================

    @Nested
    @DisplayName("GET /api/expense/{id} 经费详情")
    class DetailTests {

        @Test
        @DisplayName("未登录应返回 401")
        void notLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseController.detail(REQUEST_ID));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(approvalAuthorizationService);
            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("权限校验失败应返回 403，不返回详情")
        void noAccess_ShouldReturn403_AndNotQueryDetail() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该经费申请"))
                    .when(approvalAuthorizationService).requireReadableExpense(REQUEST_ID, stranger);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseController.detail(REQUEST_ID));
            assertEquals(403, ex.getCode());
            verify(expenseService, never()).getExpenseDetail(anyLong());
        }

        @Test
        @DisplayName("权限通过后应返回详情")
        void authorized_ShouldReturnDetail() {
            User applicant = user(USER_ID, "EMPLOYEE");
            ExpenseRequest req = new ExpenseRequest();
            req.setId(REQUEST_ID);
            when(userService.getLoginUser()).thenReturn(applicant);
            when(approvalAuthorizationService.requireReadableExpense(REQUEST_ID, applicant)).thenReturn(req);
            when(expenseService.getExpenseDetail(REQUEST_ID)).thenReturn(req);

            var result = expenseController.detail(REQUEST_ID);

            assertEquals(200, result.getCode());
            assertEquals(REQUEST_ID, result.getData().getId());
            verify(expenseService).getExpenseDetail(REQUEST_ID);
        }

        @Test
        @DisplayName("无关 EMPLOYEE 不应获得附件 URL")
        void unrelatedEmployee_ShouldNotGetReceiptUrl() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该经费申请"))
                    .when(approvalAuthorizationService).requireReadableExpense(REQUEST_ID, stranger);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseController.detail(REQUEST_ID));
            assertEquals(403, ex.getCode());
            // 确保详情接口从未被调用，附件 URL 不会被返回
            verify(expenseService, never()).getExpenseDetail(anyLong());
        }
    }

    // ======================== 审计日志 ========================

    @Nested
    @DisplayName("GET /api/expense/{id}/audit-logs 审计日志")
    class AuditLogsTests {

        @Test
        @DisplayName("未登录应返回 401")
        void notLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseController.auditLogs(REQUEST_ID));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(approvalAuthorizationService);
            verify(expenseService, never()).getAuditLogs(anyLong());
        }

        @Test
        @DisplayName("无权限时审计日志不应返回")
        void noAccess_ShouldNotReturnAuditLogs() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该经费申请"))
                    .when(approvalAuthorizationService).requireReadableExpense(REQUEST_ID, stranger);

            assertThrows(BusinessException.class,
                    () -> expenseController.auditLogs(REQUEST_ID));
            verify(expenseService, never()).getAuditLogs(anyLong());
        }

        @Test
        @DisplayName("权限通过后应返回审计日志")
        void authorized_ShouldReturnAuditLogs() {
            User applicant = user(USER_ID, "EMPLOYEE");
            ExpenseRequest req = new ExpenseRequest();
            req.setId(REQUEST_ID);
            when(userService.getLoginUser()).thenReturn(applicant);
            when(approvalAuthorizationService.requireReadableExpense(REQUEST_ID, applicant)).thenReturn(req);
            when(expenseService.getAuditLogs(REQUEST_ID)).thenReturn(List.of());

            var result = expenseController.auditLogs(REQUEST_ID);

            assertEquals(200, result.getCode());
            verify(expenseService).getAuditLogs(REQUEST_ID);
        }
    }

    // ======================== 审批任务 ========================

    @Nested
    @DisplayName("GET /api/expense/{id}/tasks 审批任务")
    class TasksTests {

        @Test
        @DisplayName("未登录应返回 401")
        void notLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseController.tasks(REQUEST_ID));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(approvalAuthorizationService);
            verify(expenseService, never()).getApprovalTasks(anyLong());
        }

        @Test
        @DisplayName("无权限时审批任务不应返回")
        void noAccess_ShouldNotReturnTasks() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该经费申请"))
                    .when(approvalAuthorizationService).requireReadableExpense(REQUEST_ID, stranger);

            assertThrows(BusinessException.class,
                    () -> expenseController.tasks(REQUEST_ID));
            verify(expenseService, never()).getApprovalTasks(anyLong());
        }

        @Test
        @DisplayName("权限通过后应返回审批任务")
        void authorized_ShouldReturnTasks() {
            User approver = user(USER_ID, "EMPLOYEE");
            ExpenseRequest req = new ExpenseRequest();
            req.setId(REQUEST_ID);
            when(userService.getLoginUser()).thenReturn(approver);
            when(approvalAuthorizationService.requireReadableExpense(REQUEST_ID, approver)).thenReturn(req);
            when(expenseService.getApprovalTasks(REQUEST_ID)).thenReturn(List.of());

            var result = expenseController.tasks(REQUEST_ID);

            assertEquals(200, result.getCode());
            verify(expenseService).getApprovalTasks(REQUEST_ID);
        }
    }

    // ======================== 列表接口不应受影响 ========================

    @Nested
    @DisplayName("列表接口不应受影响")
    class ListEndpointsUnchangedTests {

        @Test
        @DisplayName("my-expenses 应正常工作")
        void myExpenses_ShouldWork() {
            User user = user(USER_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(user);
            when(expenseService.getMyExpenses(USER_ID)).thenReturn(List.of());

            var result = expenseController.myExpenses();

            assertEquals(200, result.getCode());
            verify(expenseService).getMyExpenses(USER_ID);
        }

        @Test
        @DisplayName("pending 应正常工作")
        void pending_ShouldWork() {
            User user = user(USER_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(user);
            when(expenseService.getPendingExpenses(USER_ID)).thenReturn(List.of());

            var result = expenseController.pending();

            assertEquals(200, result.getCode());
            verify(expenseService).getPendingExpenses(USER_ID);
        }
    }
}
