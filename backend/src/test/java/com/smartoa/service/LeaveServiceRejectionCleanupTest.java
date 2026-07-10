package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.mapper.ApprovalTaskMapper;
import com.smartoa.mapper.LeaveRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveService 驳回任务清理测试（Mockito）")
class LeaveServiceRejectionCleanupTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private ApprovalTaskMapper approvalTaskMapper;

    @InjectMocks
    private LeaveService leaveService;

    @Nested
    @DisplayName("驳回后任务清理")
    class RejectionCleanupTests {

        @Test
        @DisplayName("驳回时应使用驳回前的节点 ID 跳过并行任务")
        void testReject_ShouldSkipTasksWithOriginalNodeId() {
            // Arrange
            LeaveRequest request = new LeaveRequest();
            request.setId(1L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(10L);  // 节点ID = 10
            request.setCurrentApproverId(2L);
            request.setApprovalStep(1);

            ApprovalTask task = new ApprovalTask();
            task.setId(100L);
            task.setLeaveRequestId(1L);
            task.setNodeId(10L);
            task.setApproverId(2L);
            task.setStatus("PENDING");

            when(leaveRequestMapper.selectById(1L)).thenReturn(request);
            when(approvalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

            // Act
            leaveService.approveLeave(1L, 2L, "REJECT", "不同意");

            // Assert
            ArgumentCaptor<Long> nodeIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(approvalTaskMapper).update(
                    isNull(),
                    any()
            );

            // 验证 updateById 被调用，request 已被更新
            verify(leaveRequestMapper).updateById(request);
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
        }

        @Test
        @DisplayName("驳回时 currentNodeId 在清空前应已保存")
        void testReject_ShouldPreserveNodeIdBeforeCleanup() {
            // Arrange
            LeaveRequest request = new LeaveRequest();
            request.setId(2L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(20L);
            request.setCurrentApproverId(3L);
            request.setApprovalStep(1);

            ApprovalTask task = new ApprovalTask();
            task.setId(200L);
            task.setLeaveRequestId(2L);
            task.setNodeId(20L);
            task.setApproverId(3L);
            task.setStatus("PENDING");

            when(leaveRequestMapper.selectById(2L)).thenReturn(request);
            when(approvalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

            // Act
            leaveService.approveLeave(2L, 3L, "REJECT", null);

            // Assert
            // 验证清理后状态正确
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
        }

        @Test
        @DisplayName("SINGLE 审批（无并行任务）驳回时也应正常工作")
        void testReject_SingleApproval_ShouldWorkWithoutParallelTasks() {
            // Arrange
            LeaveRequest request = new LeaveRequest();
            request.setId(3L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(30L);
            request.setCurrentApproverId(4L);
            request.setApprovalStep(1);

            when(leaveRequestMapper.selectById(3L)).thenReturn(request);
            when(approvalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // Act
            leaveService.approveLeave(3L, 4L, "REJECT", "不同意");

            // Assert
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
            // SINGLE 模式没有并行任务，不应调用 update
            verify(approvalTaskMapper, never()).update(
                    isNull(),
                    any()
            );
        }
    }
}
