package com.smartoa.service;

import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.mapper.ApprovalNodeMapper;
import com.smartoa.mapper.ApprovalRecordMapper;
import com.smartoa.mapper.ApprovalTaskMapper;
import com.smartoa.mapper.LeaveRequestMapper;
import com.smartoa.mapper.UserMapper;
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
@DisplayName("LeaveService 驳回任务清理测试（Mockito）")
class LeaveServiceRejectionCleanupTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @Mock
    private ApprovalTaskMapper approvalTaskMapper;

    @Mock
    private ApprovalNodeMapper approvalNodeMapper;

    @Mock
    private UserMapper userMapper;

    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        leaveService = new LeaveService(
                leaveRequestMapper,
                approvalRecordMapper,
                approvalNodeMapper,
                approvalTaskMapper,
                userMapper
        );
    }

    @Nested
    @DisplayName("驳回后任务清理")
    class RejectionCleanupTests {

        @Test
        @DisplayName("并行审批驳回：当前任务标记为COMPLETED，同节点剩余PENDING任务标记为SKIPPED")
        void testReject_ParallelApproval_ShouldCleanupTasks() {
            // Arrange
            LeaveRequest request = new LeaveRequest();
            request.setId(1L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(10L);
            request.setCurrentApproverId(2L);
            request.setApprovalStep(1);
            request.setTimeoutTime(java.time.LocalDateTime.now().plusHours(24));

            ApprovalTask task = new ApprovalTask();
            task.setId(100L);
            task.setLeaveRequestId(1L);
            task.setNodeId(10L);
            task.setApproverId(2L);
            task.setStatus("PENDING");

            when(leaveRequestMapper.selectById(1L)).thenReturn(request);
            when(approvalTaskMapper.selectOne(any())).thenReturn(task);
            when(approvalTaskMapper.skipPendingByRequestAndNode(1L, 10L)).thenReturn(2);

            // Act
            leaveService.approveLeave(1L, 2L, "REJECT", "不同意");

            // Assert - 验证当前任务状态
            verify(approvalTaskMapper).updateById(argThat((ApprovalTask t) ->
                    "COMPLETED".equals(t.getStatus()) &&
                    Long.valueOf(100L).equals(t.getId())
            ));

            // 验证剩余 PENDING 任务使用驳回前的节点ID清理
            verify(approvalTaskMapper).skipPendingByRequestAndNode(1L, 10L);

            // 验证申请状态和字段清理
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
            assertNull(request.getTimeoutTime());

            // 验证审批记录被写入
            verify(approvalRecordMapper).insert(argThat((com.smartoa.entity.ApprovalRecord record) ->
                    "REJECT".equals(record.getAction()) &&
                    Long.valueOf(10L).equals(record.getNodeId()) &&
                    Long.valueOf(1L).equals(record.getLeaveRequestId())
            ));
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
            when(approvalTaskMapper.selectOne(any())).thenReturn(null);

            // Act
            leaveService.approveLeave(3L, 4L, "REJECT", "不同意");

            // Assert - SINGLE 模式没有并行任务，不应调用批量清理
            verify(approvalTaskMapper, never()).skipPendingByRequestAndNode(anyLong(), anyLong());

            // 验证申请状态
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());

            // 验证审批记录被写入
            verify(approvalRecordMapper).insert(argThat((com.smartoa.entity.ApprovalRecord record) ->
                    "REJECT".equals(record.getAction()) &&
                    Long.valueOf(30L).equals(record.getNodeId()) &&
                    Long.valueOf(3L).equals(record.getLeaveRequestId())
            ));
        }

        @Test
        @DisplayName("驳回时 currentNodeId 应为驳回前的值（不为null）")
        void testReject_ShouldUseOriginalNodeIdBeforeCleanup() {
            // Arrange
            LeaveRequest request = new LeaveRequest();
            request.setId(4L);
            request.setStatus("PENDING");
            request.setCurrentNodeId(40L);
            request.setCurrentApproverId(5L);
            request.setApprovalStep(1);

            ApprovalTask task = new ApprovalTask();
            task.setId(400L);
            task.setLeaveRequestId(4L);
            task.setNodeId(40L);
            task.setApproverId(5L);
            task.setStatus("PENDING");

            when(leaveRequestMapper.selectById(4L)).thenReturn(request);
            when(approvalTaskMapper.selectOne(any())).thenReturn(task);
            when(approvalTaskMapper.skipPendingByRequestAndNode(4L, 40L)).thenReturn(1);

            // Act
            leaveService.approveLeave(4L, 5L, "REJECT", "不同意");

            // Assert - 验证使用驳回前的节点ID（40L），而不是清理后的 null
            verify(approvalTaskMapper).skipPendingByRequestAndNode(4L, 40L);

            // 验证申请状态已清理
            assertEquals("REJECTED", request.getStatus());
            assertNull(request.getCurrentNodeId());
            assertNull(request.getCurrentApproverId());
        }
    }
}
