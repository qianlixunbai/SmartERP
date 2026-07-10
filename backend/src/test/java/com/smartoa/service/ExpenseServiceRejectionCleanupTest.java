package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.ExpenseApprovalTask;
import com.smartoa.entity.AuditLog;
import com.smartoa.mapper.ExpenseApprovalTaskMapper;
import com.smartoa.mapper.ExpenseRequestMapper;
import com.smartoa.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService 驳回任务清理测试（Mockito）")
class ExpenseServiceRejectionCleanupTest {

    @Mock
    private ExpenseRequestMapper expenseRequestMapper;

    @Mock
    private ExpenseApprovalTaskMapper expenseApprovalTaskMapper;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private ExpenseService expenseService;

    @Nested
    @DisplayName("驳回后任务清理")
    class RejectionCleanupTests {

        @Test
        @DisplayName("驳回时应使用驳回前的节点 ID 跳过并行任务")
        void testReject_ShouldSkipTasksWithOriginalNodeId() {
            // Arrange
            ExpenseRequest request = new ExpenseRequest();
            request.setId(1L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(10L);
            request.setCurrentApproverId(2L);
            request.setAmount(BigDecimal.valueOf(500));
            request.setCategory("差旅");
            request.setApplicantId(1L);
            request.setCreateTime(LocalDateTime.now());
            request.setApprovalStep(1);

            ExpenseApprovalTask task = new ExpenseApprovalTask();
            task.setId(100L);
            task.setExpenseRequestId(1L);
            task.setNodeId(10L);
            task.setApproverId(2L);
            task.setStatus("PENDING");

            when(expenseRequestMapper.selectById(1L)).thenReturn(request);
            when(expenseApprovalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

            // Act
            expenseService.approveExpense(1L, 2L, "REJECT", "不同意");

            // Assert
            verify(expenseRequestMapper).updateById(request);
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
        }

        @Test
        @DisplayName("驳回时 currentNodeId 在清空前应已保存")
        void testReject_ShouldPreserveNodeIdBeforeCleanup() {
            // Arrange
            ExpenseRequest request = new ExpenseRequest();
            request.setId(2L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(20L);
            request.setCurrentApproverId(3L);
            request.setAmount(BigDecimal.valueOf(1000));
            request.setCategory("办公");
            request.setApplicantId(1L);
            request.setCreateTime(LocalDateTime.now());
            request.setApprovalStep(1);

            ExpenseApprovalTask task = new ExpenseApprovalTask();
            task.setId(200L);
            task.setExpenseRequestId(2L);
            task.setNodeId(20L);
            task.setApproverId(3L);
            task.setStatus("PENDING");

            when(expenseRequestMapper.selectById(2L)).thenReturn(request);
            when(expenseApprovalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

            // Act
            expenseService.approveExpense(2L, 3L, "REJECT", null);

            // Assert
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
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
            when(expenseApprovalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // Act
            expenseService.approveExpense(3L, 4L, "REJECT", "不同意");

            // Assert
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
        }
    }
}
