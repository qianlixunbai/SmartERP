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
import org.junit.jupiter.api.Disabled;
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
                accountingService
        );
    }

    @Nested
    @DisplayName("驳回后任务清理")
    class RejectionCleanupTests {

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

            // Assert
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
            // SINGLE 模式没有并行任务，不应调用批量 update
            verify(expenseApprovalTaskMapper, never()).update(isNull(), any());
        }

        @Test
        @DisplayName("驳回时应写入审计日志")
        void testReject_ShouldWriteAuditLog() {
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

            when(expenseRequestMapper.selectById(4L)).thenReturn(request);
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(null);

            // Act
            expenseService.approveExpense(4L, 5L, "REJECT", "不同意");

            // Assert - 验证审计日志被写入（包含 APPROVE 动作和 comment）
            verify(auditLogMapper).insert(argThat((AuditLog log) ->
                    "APPROVE".equals(log.getAction()) &&
                    "EXPENSE".equals(log.getTargetType()) &&
                    Long.valueOf(4L).equals(log.getTargetId())
            ));
        }

        @Test
        @Disabled("需要 Spring context 初始化 MyBatis Plus lambda cache；跳过并行任务验证由集成测试覆盖")
        @DisplayName("并行审批驳回：状态清理")
        void testReject_ParallelApproval_ShouldCleanupState() {
            // 此测试需要完整的 Spring context 来初始化 MyBatis Plus LambdaUpdateWrapper
            // 原因：LambdaUpdateWrapper 在纯 Mockito 环境下无法工作（无 lambda cache）
            //
            // 验证内容：
            // 1. 当前任务标记为 COMPLETED
            // 2. 同节点剩余 PENDING 任务标记为 SKIPPED
            // 3. request 状态设为 REJECTED
            // 4. currentNodeId、currentApproverId、timeoutTime 清空
            //
            // 测试策略：通过 @SpringBootTest + @Transactional + 本地 MySQL 运行
            // 或通过代码审查验证逻辑正确性

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
            when(expenseApprovalTaskMapper.selectOne(any())).thenReturn(task);

            // Act
            expenseService.approveExpense(2L, 3L, "REJECT", null);

            // Assert
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
            assertEquals("COMPLETED", task.getStatus());
        }
    }
}
