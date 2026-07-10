package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalAuthorizationService 对象级权限测试（Mockito）")
class ApprovalAuthorizationServiceTest {

    private static final Long REQUEST_ID = 42L;
    private static final Long APPLICANT_ID = 1L;
    private static final Long APPROVER_ID = 2L;
    private static final Long UNRELATED_ID = 99L;

    @Mock
    private LeaveRequestMapper leaveRequestMapper;
    @Mock
    private ExpenseRequestMapper expenseRequestMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private ApprovalTaskMapper approvalTaskMapper;
    @Mock
    private ExpenseApprovalTaskMapper expenseApprovalTaskMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private ApprovalAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalAuthorizationService(
                leaveRequestMapper,
                expenseRequestMapper,
                approvalRecordMapper,
                approvalTaskMapper,
                expenseApprovalTaskMapper,
                auditLogMapper
        );
    }

    // ======================== 辅助方法 ========================

    private User user(Long id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    private LeaveRequest leaveRequest(Long id, Long applicantId, Long currentApproverId) {
        LeaveRequest r = new LeaveRequest();
        r.setId(id);
        r.setApplicantId(applicantId);
        r.setCurrentApproverId(currentApproverId);
        return r;
    }

    private ExpenseRequest expenseRequest(Long id, Long applicantId, Long currentApproverId) {
        ExpenseRequest r = new ExpenseRequest();
        r.setId(id);
        r.setApplicantId(applicantId);
        r.setCurrentApproverId(currentApproverId);
        return r;
    }

    // ======================== 请假权限 ========================

    @Nested
    @DisplayName("requireReadableLeave 权限校验")
    class LeaveTests {

        @Test
        @DisplayName("用户为 null 应抛 401")
        void nullUser_ShouldThrow401() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableLeave(REQUEST_ID, null));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(leaveRequestMapper);
        }

        @Test
        @DisplayName("用户非 null 但 id 为 null 应抛 401，不查询申请")
        void userWithNullId_ShouldThrow401() {
            User noId = new User();
            noId.setRole("EMPLOYEE");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableLeave(REQUEST_ID, noId));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(leaveRequestMapper);
            verifyNoInteractions(approvalRecordMapper);
            verifyNoInteractions(approvalTaskMapper);
        }

        @Test
        @DisplayName("MANAGER 但 id 为 null 应抛 401，不能放行")
        void managerWithNullId_ShouldThrow401() {
            User noIdManager = new User();
            noIdManager.setRole("MANAGER");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableLeave(REQUEST_ID, noIdManager));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(leaveRequestMapper);
        }

        @Test
        @DisplayName("申请不存在应抛 404")
        void requestNotFound_ShouldThrow404() {
            User user = user(UNRELATED_ID, "EMPLOYEE");
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableLeave(REQUEST_ID, user));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("MANAGER 应允许访问")
        void manager_ShouldAllow() {
            User manager = user(999L, "MANAGER");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

            LeaveRequest result = service.requireReadableLeave(REQUEST_ID, manager);
            assertNotNull(result);
            assertEquals(REQUEST_ID, result.getId());
        }

        @Test
        @DisplayName("申请人本人应允许访问")
        void applicant_ShouldAllow() {
            User applicant = user(APPLICANT_ID, "EMPLOYEE");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

            LeaveRequest result = service.requireReadableLeave(REQUEST_ID, applicant);
            assertNotNull(result);
            assertEquals(APPLICANT_ID, result.getApplicantId());
        }

        @Test
        @DisplayName("当前审批人应允许访问")
        void currentApprover_ShouldAllow() {
            User approver = user(APPROVER_ID, "EMPLOYEE");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

            LeaveRequest result = service.requireReadableLeave(REQUEST_ID, approver);
            assertNotNull(result);
        }

        @Test
        @DisplayName("历史审批人（有审批记录）应允许访问")
        void historicalApprover_ShouldAllow() {
            User historical = user(UNRELATED_ID, "EMPLOYEE");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(approvalRecordMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(1);

            LeaveRequest result = service.requireReadableLeave(REQUEST_ID, historical);
            assertNotNull(result);
            verify(approvalRecordMapper).countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID);
        }

        @Test
        @DisplayName("有相关审批任务的用户应允许访问")
        void userWithApprovalTask_ShouldAllow() {
            User taskHolder = user(UNRELATED_ID, "EMPLOYEE");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(approvalRecordMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);
            when(approvalTaskMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(1);

            LeaveRequest result = service.requireReadableLeave(REQUEST_ID, taskHolder);
            assertNotNull(result);
            verify(approvalTaskMapper).countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID);
        }

        @Test
        @DisplayName("无关 EMPLOYEE 应抛 403")
        void unrelatedEmployee_ShouldThrow403() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(approvalRecordMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);
            when(approvalTaskMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableLeave(REQUEST_ID, stranger));
            assertEquals(403, ex.getCode());
        }

        @Test
        @DisplayName("currentApproverId 为 null 时不应 NPE")
        void nullCurrentApproverId_ShouldNotNPE() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            LeaveRequest req = leaveRequest(REQUEST_ID, APPLICANT_ID, null);
            when(leaveRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(approvalRecordMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);
            when(approvalTaskMapper.countByLeaveRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableLeave(REQUEST_ID, stranger));
            assertEquals(403, ex.getCode());
        }
    }

    // ======================== 经费权限 ========================

    @Nested
    @DisplayName("requireReadableExpense 权限校验")
    class ExpenseTests {

        @Test
        @DisplayName("用户为 null 应抛 401")
        void nullUser_ShouldThrow401() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableExpense(REQUEST_ID, null));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(expenseRequestMapper);
        }

        @Test
        @DisplayName("用户非 null 但 id 为 null 应抛 401，不查询申请")
        void userWithNullId_ShouldThrow401() {
            User noId = new User();
            noId.setRole("EMPLOYEE");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableExpense(REQUEST_ID, noId));
            assertEquals(401, ex.getCode());
            verifyNoInteractions(expenseRequestMapper);
            verifyNoInteractions(expenseApprovalTaskMapper);
            verifyNoInteractions(auditLogMapper);
        }

        @Test
        @DisplayName("申请不存在应抛 404")
        void requestNotFound_ShouldThrow404() {
            User user = user(UNRELATED_ID, "EMPLOYEE");
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableExpense(REQUEST_ID, user));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("MANAGER 应允许访问")
        void manager_ShouldAllow() {
            User manager = user(999L, "MANAGER");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

            ExpenseRequest result = service.requireReadableExpense(REQUEST_ID, manager);
            assertNotNull(result);
            assertEquals(REQUEST_ID, result.getId());
        }

        @Test
        @DisplayName("申请人本人应允许访问")
        void applicant_ShouldAllow() {
            User applicant = user(APPLICANT_ID, "EMPLOYEE");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

            ExpenseRequest result = service.requireReadableExpense(REQUEST_ID, applicant);
            assertNotNull(result);
            assertEquals(APPLICANT_ID, result.getApplicantId());
        }

        @Test
        @DisplayName("当前审批人应允许访问")
        void currentApprover_ShouldAllow() {
            User approver = user(APPROVER_ID, "EMPLOYEE");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);

            ExpenseRequest result = service.requireReadableExpense(REQUEST_ID, approver);
            assertNotNull(result);
        }

        @Test
        @DisplayName("历史审批任务参与者应允许访问")
        void historicalTaskParticipant_ShouldAllow() {
            User taskHolder = user(UNRELATED_ID, "EMPLOYEE");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(1);

            ExpenseRequest result = service.requireReadableExpense(REQUEST_ID, taskHolder);
            assertNotNull(result);
            verify(expenseApprovalTaskMapper).countByExpenseRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID);
        }

        @Test
        @DisplayName("真实审批审计日志中的操作者应允许访问")
        void auditLogApprover_ShouldAllow() {
            User auditor = user(UNRELATED_ID, "EMPLOYEE");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);
            when(auditLogMapper.countExpenseApprovalActions(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(1);

            ExpenseRequest result = service.requireReadableExpense(REQUEST_ID, auditor);
            assertNotNull(result);
            verify(auditLogMapper).countExpenseApprovalActions(REQUEST_ID, UNRELATED_ID);
        }

        @Test
        @DisplayName("无关 EMPLOYEE 应抛 403")
        void unrelatedEmployee_ShouldThrow403() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, APPROVER_ID);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);
            when(auditLogMapper.countExpenseApprovalActions(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableExpense(REQUEST_ID, stranger));
            assertEquals(403, ex.getCode());
        }

        @Test
        @DisplayName("currentApproverId 为 null 时不应 NPE")
        void nullCurrentApproverId_ShouldNotNPE() {
            User stranger = user(UNRELATED_ID, "EMPLOYEE");
            ExpenseRequest req = expenseRequest(REQUEST_ID, APPLICANT_ID, null);
            when(expenseRequestMapper.selectById(REQUEST_ID)).thenReturn(req);
            when(expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);
            when(auditLogMapper.countExpenseApprovalActions(REQUEST_ID, UNRELATED_ID))
                    .thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requireReadableExpense(REQUEST_ID, stranger));
            assertEquals(403, ex.getCode());
        }
    }
}
