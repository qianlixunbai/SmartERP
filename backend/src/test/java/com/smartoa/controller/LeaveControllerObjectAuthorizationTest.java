package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.service.ApprovalAuthorizationService;
import com.smartoa.service.LeaveService;
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
@DisplayName("LeaveController 对象级权限测试（Mockito）")
class LeaveControllerObjectAuthorizationTest {

    private static final Long REQUEST_ID = 42L;
    private static final Long USER_ID = 1L;
    private static final Long UNRELATED_ID = 99L;

    @Mock
    private LeaveService leaveService;
    @Mock
    private UserService userService;
    @Mock
    private ApprovalAuthorizationService approvalAuthorizationService;

    private LeaveController leaveController;

    @BeforeEach
    void setUp() {
        leaveController = new LeaveController(leaveService, userService, approvalAuthorizationService);
    }

    // ======================== 辅助方法 ========================

    private User user(Long id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    // ======================== 请假详情 ========================

    @Nested
    @DisplayName("GET /api/leave/{id} 请假详情")
    class GetRequestDetailTests {

        @Test
        @DisplayName("未登录应返回 401")
        void notLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> leaveController.getRequestDetail(REQUEST_ID));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(approvalAuthorizationService);
            verifyNoInteractions(leaveService);
        }

        @Test
        @DisplayName("权限校验失败应返回 403，不查询详情")
        void noAccess_ShouldReturn403_AndNotQueryDetail() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该请假单"))
                    .when(approvalAuthorizationService).requireReadableLeave(REQUEST_ID, stranger);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> leaveController.getRequestDetail(REQUEST_ID));
            assertEquals(403, ex.getCode());
            verify(leaveService, never()).getRequestDetail(anyLong());
        }

        @Test
        @DisplayName("权限通过后应返回详情")
        void authorized_ShouldReturnDetail() {
            User applicant = user(USER_ID, "EMPLOYEE");
            LeaveRequest req = new LeaveRequest();
            req.setId(REQUEST_ID);
            when(userService.getLoginUser()).thenReturn(applicant);
            when(approvalAuthorizationService.requireReadableLeave(REQUEST_ID, applicant)).thenReturn(req);
            when(leaveService.getRequestDetail(REQUEST_ID)).thenReturn(req);

            var result = leaveController.getRequestDetail(REQUEST_ID);

            assertEquals(200, result.getCode());
            assertEquals(REQUEST_ID, result.getData().getId());
            verify(leaveService).getRequestDetail(REQUEST_ID);
        }
    }

    // ======================== 审批记录 ========================

    @Nested
    @DisplayName("GET /api/leave/{id}/records 审批记录")
    class GetApprovalRecordsTests {

        @Test
        @DisplayName("未登录应返回 401")
        void notLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> leaveController.getApprovalRecords(REQUEST_ID));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(approvalAuthorizationService);
            verify(leaveService, never()).getApprovalRecords(anyLong());
        }

        @Test
        @DisplayName("无权限时审批记录 Mapper 不应被 Controller 调用")
        void noAccess_ShouldNotQueryRecords() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该请假单"))
                    .when(approvalAuthorizationService).requireReadableLeave(REQUEST_ID, stranger);

            assertThrows(BusinessException.class,
                    () -> leaveController.getApprovalRecords(REQUEST_ID));
            verify(leaveService, never()).getApprovalRecords(anyLong());
        }

        @Test
        @DisplayName("权限通过后应返回审批记录")
        void authorized_ShouldReturnRecords() {
            User approver = user(USER_ID, "EMPLOYEE");
            LeaveRequest req = new LeaveRequest();
            req.setId(REQUEST_ID);
            when(userService.getLoginUser()).thenReturn(approver);
            when(approvalAuthorizationService.requireReadableLeave(REQUEST_ID, approver)).thenReturn(req);
            when(leaveService.getApprovalRecords(REQUEST_ID)).thenReturn(List.of());

            var result = leaveController.getApprovalRecords(REQUEST_ID);

            assertEquals(200, result.getCode());
            verify(leaveService).getApprovalRecords(REQUEST_ID);
        }
    }

    // ======================== 审批任务 ========================

    @Nested
    @DisplayName("GET /api/leave/{id}/tasks 审批任务")
    class GetPendingTasksTests {

        @Test
        @DisplayName("未登录应返回 401")
        void notLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> leaveController.getPendingTasks(REQUEST_ID));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(approvalAuthorizationService);
            verify(leaveService, never()).getPendingTasks(anyLong());
        }

        @Test
        @DisplayName("无权限时任务 Mapper 不应被 Controller 调用")
        void noAccess_ShouldNotQueryTasks() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(stranger);
            doThrow(new BusinessException(403, "无权限访问该请假单"))
                    .when(approvalAuthorizationService).requireReadableLeave(REQUEST_ID, stranger);

            assertThrows(BusinessException.class,
                    () -> leaveController.getPendingTasks(REQUEST_ID));
            verify(leaveService, never()).getPendingTasks(anyLong());
        }

        @Test
        @DisplayName("权限通过后应返回任务列表")
        void authorized_ShouldReturnTasks() {
            User approver = user(USER_ID, "EMPLOYEE");
            LeaveRequest req = new LeaveRequest();
            req.setId(REQUEST_ID);
            when(userService.getLoginUser()).thenReturn(approver);
            when(approvalAuthorizationService.requireReadableLeave(REQUEST_ID, approver)).thenReturn(req);
            when(leaveService.getPendingTasks(REQUEST_ID)).thenReturn(List.of());

            var result = leaveController.getPendingTasks(REQUEST_ID);

            assertEquals(200, result.getCode());
            verify(leaveService).getPendingTasks(REQUEST_ID);
        }
    }

    // ======================== 列表接口不应受影响 ========================

    @Nested
    @DisplayName("列表接口不应受影响")
    class ListEndpointsUnchangedTests {

        @Test
        @DisplayName("my-requests 应正常工作")
        void myRequests_ShouldWork() {
            User user = user(USER_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(user);
            when(leaveService.getMyRequests(USER_ID)).thenReturn(List.of());

            var result = leaveController.getMyRequests();

            assertEquals(200, result.getCode());
            verify(leaveService).getMyRequests(USER_ID);
        }

        @Test
        @DisplayName("pending 应正常工作")
        void pending_ShouldWork() {
            User user = user(USER_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(user);
            when(leaveService.getPendingRequests(USER_ID)).thenReturn(List.of());

            var result = leaveController.getPendingRequests();

            assertEquals(200, result.getCode());
            verify(leaveService).getPendingRequests(USER_ID);
        }

        @Test
        @DisplayName("done 应正常工作")
        void done_ShouldWork() {
            User user = user(USER_ID, "EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(user);
            when(leaveService.getDoneRequests(USER_ID)).thenReturn(List.of());

            var result = leaveController.getDoneRequests();

            assertEquals(200, result.getCode());
            verify(leaveService).getDoneRequests(USER_ID);
        }
    }
}
