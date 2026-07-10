package com.smartoa.service;

import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.ExpenseApprovalTask;
import com.smartoa.entity.AuditLog;
import com.smartoa.mapper.ApprovalNodeMapper;
import com.smartoa.mapper.ExpenseApprovalTaskMapper;
import com.smartoa.mapper.ExpenseRequestMapper;
import com.smartoa.mapper.AuditLogMapper;
import com.smartoa.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService 驳回任务清理测试（Mockito）")
class ExpenseServiceRejectionCleanupTest {

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

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
                expenseRequestMapper,
                expenseApprovalTaskMapper,
                approvalNodeMapper,
                userMapper,
                auditLogMapper,
                accountingService,
                new ApprovalConditionEvaluator()
        );
    }

    @Nested
    @DisplayName("驳回后任务清理")
    class RejectionCleanupTests {

        @Test
        @DisplayName("并行审批驳回：当前任务标记为COMPLETED，同节点剩余PENDING任务标记为SKIPPED")
        void testReject_ParallelApproval_ShouldCleanupTasks() {
            // Arrange
            ExpenseRequest request = new ExpenseRequest();
            request.setId(1L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(10L);
            request.setCurrentApproverId(2L);
            request.setAmount(BigDecimal.valueOf(1000));
            request.setCategory("差旅");
            request.setApplicantId(1L);
            request.setCreateTime(LocalDateTime.now());
            request.setApprovalStep(1);
            request.setTimeoutTime(LocalDateTime.now().plusHours(24));

            ExpenseApprovalTask task = new ExpenseApprovalTask();
            task.setId(100L);
            task.setExpenseRequestId(1L);
            task.setNodeId(10L);
            task.setApproverId(2L);
            task.setStatus("PENDING");

            when(expenseRequestMapper.selectById(1L)).thenReturn(request);
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(task);
            when(expenseApprovalTaskMapper.skipPendingByRequestAndNode(1L, 10L)).thenReturn(2);

            // Act
            expenseService.approveExpense(1L, 2L, "REJECT", "不同意");

            // Assert - 验证当前任务状态
            verify(expenseApprovalTaskMapper).updateById(argThat((ExpenseApprovalTask t) ->
                    "COMPLETED".equals(t.getStatus()) &&
                    Long.valueOf(100L).equals(t.getId())
            ));

            // 验证剩余 PENDING 任务使用驳回前的节点ID清理
            verify(expenseApprovalTaskMapper).skipPendingByRequestAndNode(1L, 10L);

            // 验证申请状态和字段清理
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
            assertNull(request.getTimeoutTime());

            // 验证审计日志被写入，action 应为真实操作 REJECT
            verify(auditLogMapper).insert(argThat((AuditLog log) ->
                    "REJECT".equals(log.getAction()) &&
                    "EXPENSE".equals(log.getTargetType()) &&
                    Long.valueOf(1L).equals(log.getTargetId())
            ));

            // 验证未触发财务入账
            verify(accountingService, never()).post(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("SINGLE 审批驳回时也应正常工作")
        void testReject_SingleApproval_ShouldWorkWithoutParallelTasks() {
            // Arrange
            ExpenseRequest request = new ExpenseRequest();
            request.setId(3L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(30L);
            request.setCurrentApproverId(4L);
            request.setAmount(BigDecimal.valueOf(2000));
            request.setCategory("培训");
            request.setApplicantId(1L);
            request.setCreateTime(LocalDateTime.now());
            request.setApprovalStep(1);

            when(expenseRequestMapper.selectById(3L)).thenReturn(request);
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(null);

            // Act
            expenseService.approveExpense(3L, 4L, "REJECT", "不同意");

            // Assert - SINGLE 模式没有并行任务，不应调用批量清理
            verify(expenseApprovalTaskMapper, never()).skipPendingByRequestAndNode(anyLong(), anyLong());

            // 验证申请状态
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());

            // 验证审计日志被写入，action 应为真实操作 REJECT
            verify(auditLogMapper).insert(argThat((AuditLog log) ->
                    "REJECT".equals(log.getAction()) &&
                    "EXPENSE".equals(log.getTargetType()) &&
                    Long.valueOf(3L).equals(log.getTargetId())
            ));
        }

        @Test
        @DisplayName("驳回时 currentNodeId 应为驳回前的值（不为null）")
        void testReject_ShouldUseOriginalNodeIdBeforeCleanup() {
            // Arrange
            ExpenseRequest request = new ExpenseRequest();
            request.setId(4L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(40L);
            request.setCurrentApproverId(5L);
            request.setAmount(BigDecimal.valueOf(3000));
            request.setCategory("设备");
            request.setApplicantId(1L);
            request.setCreateTime(LocalDateTime.now());
            request.setApprovalStep(1);

            ExpenseApprovalTask task = new ExpenseApprovalTask();
            task.setId(400L);
            task.setExpenseRequestId(4L);
            task.setNodeId(40L);
            task.setApproverId(5L);
            task.setStatus("PENDING");

            when(expenseRequestMapper.selectById(4L)).thenReturn(request);
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(task);
            when(expenseApprovalTaskMapper.skipPendingByRequestAndNode(4L, 40L)).thenReturn(1);

            // Act
            expenseService.approveExpense(4L, 5L, "REJECT", "不同意");

            // Assert - 验证使用驳回前的节点ID（40L），而不是清理后的 null
            verify(expenseApprovalTaskMapper).skipPendingByRequestAndNode(4L, 40L);

            // 验证申请状态已清理
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
        }
    }
}
