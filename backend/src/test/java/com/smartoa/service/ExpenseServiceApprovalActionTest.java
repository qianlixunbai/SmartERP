package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.AuditLog;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService 审批动作校验测试（Mockito）")
class ExpenseServiceApprovalActionTest {

    @Mock
    private ExpenseRequestMapper expenseRequestMapper;

    @Mock
    private ExpenseApprovalTaskMapper expenseApprovalTaskMapper;

    @Mock
    private ApprovalNodeMapper approvalNodeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private AccountingService accountingService;

    @Mock
    private ApprovalConditionEvaluator conditionEvaluator;

    @InjectMocks
    private ExpenseService expenseService;

    private ExpenseRequest pendingRequest;

    @BeforeEach
    void setUp() {
        pendingRequest = new ExpenseRequest();
        pendingRequest.setId(1L);
        pendingRequest.setStatus("PENDING");
        pendingRequest.setCurrentNodeId(10L);
        pendingRequest.setCurrentApproverId(2L);
        pendingRequest.setApplicantId(1L);
        pendingRequest.setAmount(BigDecimal.valueOf(1000));
        pendingRequest.setCategory("差旅");
        pendingRequest.setApprovalStep(1);
    }

    @Nested
    @DisplayName("审批动作验证")
    class ActionValidationTests {

        @Test
        @DisplayName("APPROVE 应被接受")
        void testApprove_ShouldPass() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            // APPROVE 应该通过动作校验（可能因其他原因失败）
            try {
                expenseService.approveExpense(1L, 2L, "APPROVE", "同意");
            } catch (BusinessException e) {
                assertFalse(e.getMessage().contains("审批动作仅支持"),
                        "APPROVE 动作应该通过校验");
            }
        }

        @Test
        @DisplayName("REJECT 应被接受")
        void testReject_ShouldPass() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            // REJECT 应该通过动作校验
            try {
                expenseService.approveExpense(1L, 2L, "REJECT", "不同意");
            } catch (BusinessException e) {
                assertFalse(e.getMessage().contains("审批动作仅支持"),
                        "REJECT 动作应该通过校验");
            }
        }

        @Test
        @DisplayName("null 动作应被拒绝")
        void testNullAction_ShouldReject() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> expenseService.approveExpense(1L, 2L, null, "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("空字符串应被拒绝")
        void testEmptyAction_ShouldReject() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> expenseService.approveExpense(1L, 2L, "", "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("DELETE 动作应被拒绝")
        void testDeleteAction_ShouldReject() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> expenseService.approveExpense(1L, 2L, "DELETE", "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("小写 approve 应被拒绝")
        void testLowercaseApprove_ShouldReject() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> expenseService.approveExpense(1L, 2L, "approve", "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("非法动作不应写入审计日志")
        void testInvalidAction_ShouldNotWriteAuditLog() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            try {
                expenseService.approveExpense(1L, 2L, "DELETE", "测试");
                fail("应抛出异常");
            } catch (BusinessException e) {
                // 验证不应有任何写操作
                verify(expenseRequestMapper, never()).updateById(any(ExpenseRequest.class));
                verify(auditLogMapper, never()).insert(any(AuditLog.class));
            }
        }

        @Test
        @DisplayName("非法动作不应触发自动入账")
        void testInvalidAction_ShouldNotTriggerPosting() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            try {
                expenseService.approveExpense(1L, 2L, "INVALID", "测试");
                fail("应抛出异常");
            } catch (BusinessException e) {
                // 验证不会执行到推进节点的逻辑
                verify(approvalNodeMapper, never()).selectList(any());
                verify(accountingService, never()).post(any(), any(), any(), any(), any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("审计日志 action 语义")
    class AuditActionTests {

        @Test
        @DisplayName("APPROVE 应写入 action=APPROVE 的审计日志")
        void testApprove_ShouldWriteApproveAuditLog() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);
            // 单人审批，无并行任务
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(null);
            // 无审批节点 → finalizeApproval
            when(approvalNodeMapper.selectList(any())).thenReturn(java.util.List.of());

            expenseService.approveExpense(1L, 2L, "APPROVE", "同意");

            verify(auditLogMapper).insert(argThat((AuditLog log) ->
                    "APPROVE".equals(log.getAction()) &&
                    "EXPENSE".equals(log.getTargetType()) &&
                    Long.valueOf(1L).equals(log.getTargetId()) &&
                    Long.valueOf(2L).equals(log.getActorId())
            ));
        }

        @Test
        @DisplayName("REJECT 应写入 action=REJECT 的审计日志")
        void testReject_ShouldWriteRejectAuditLog() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(null);

            expenseService.approveExpense(1L, 2L, "REJECT", "不同意");

            verify(auditLogMapper).insert(argThat((AuditLog log) ->
                    "REJECT".equals(log.getAction()) &&
                    "EXPENSE".equals(log.getTargetType()) &&
                    Long.valueOf(1L).equals(log.getTargetId()) &&
                    Long.valueOf(2L).equals(log.getActorId())
            ));
        }

        @Test
        @DisplayName("REJECT 不应触发 AccountingService.post()")
        void testReject_ShouldNotTriggerPosting() {
            when(expenseRequestMapper.selectById(1L)).thenReturn(pendingRequest);
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(null);

            expenseService.approveExpense(1L, 2L, "REJECT", "不同意");

            verify(accountingService, never()).post(any(), any(), any(), any(), any(), any(), any());
        }
    }
}
