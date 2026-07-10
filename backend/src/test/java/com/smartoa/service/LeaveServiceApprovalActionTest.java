package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveService 审批动作校验测试（Mockito）")
class LeaveServiceApprovalActionTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @Mock
    private ApprovalNodeMapper approvalNodeMapper;

    @Mock
    private ApprovalTaskMapper approvalTaskMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private LeaveService leaveService;

    private LeaveRequest pendingRequest;

    @BeforeEach
    void setUp() {
        pendingRequest = new LeaveRequest();
        pendingRequest.setId(1L);
        pendingRequest.setStatus("PENDING");
        pendingRequest.setCurrentNodeId(10L);
        pendingRequest.setCurrentApproverId(2L);
        pendingRequest.setApplicantId(1L);
        pendingRequest.setApprovalStep(1);
    }

    @Nested
    @DisplayName("审批动作验证")
    class ActionValidationTests {

        @Test
        @DisplayName("APPROVE 应被接受")
        void testApprove_ShouldPass() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);
            when(approvalTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // APPROVE 应该正常执行（会因缺少后续逻辑抛异常，但不会因动作校验失败）
            try {
                leaveService.approveLeave(1L, 2L, "APPROVE", "同意");
            } catch (BusinessException e) {
                // 可能因审批节点找不到等其他原因失败，但不应该因为动作校验
                assertFalse(e.getMessage().contains("审批动作仅支持"),
                        "APPROVE 动作应该通过校验");
            }
        }

        @Test
        @DisplayName("REJECT 应被接受")
        void testReject_ShouldPass() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            // REJECT 应该通过动作校验
            try {
                leaveService.approveLeave(1L, 2L, "REJECT", "不同意");
            } catch (BusinessException e) {
                assertFalse(e.getMessage().contains("审批动作仅支持"),
                        "REJECT 动作应该通过校验");
            }
        }

        @Test
        @DisplayName("null 动作应被拒绝")
        void testNullAction_ShouldReject() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveService.approveLeave(1L, 2L, null, "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("空字符串应被拒绝")
        void testEmptyAction_ShouldReject() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveService.approveLeave(1L, 2L, "", "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("DELETE 动作应被拒绝")
        void testDeleteAction_ShouldReject() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveService.approveLeave(1L, 2L, "DELETE", "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("小写 approve 应被拒绝（严格大小写）")
        void testLowercaseApprove_ShouldReject() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveService.approveLeave(1L, 2L, "approve", "测试"));

            assertEquals(400, exception.getCode());
            assertEquals("审批动作仅支持 APPROVE 或 REJECT", exception.getMessage());
        }

        @Test
        @DisplayName("非法动作不应修改申请状态或写入记录")
        void testInvalidAction_ShouldNotModifyData() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            try {
                leaveService.approveLeave(1L, 2L, "DELETE", "测试");
                fail("应抛出异常");
            } catch (BusinessException e) {
                // 验证不应有任何写操作
                verify(leaveRequestMapper, never()).updateById(any(LeaveRequest.class));
                verify(approvalRecordMapper, never()).insert(any(ApprovalRecord.class));
                verify(approvalTaskMapper, never()).updateById(any(ApprovalTask.class));
            }
        }

        @Test
        @DisplayName("非法动作不应触发自动入账")
        void testInvalidAction_ShouldNotTriggerPosting() {
            when(leaveRequestMapper.selectById(1L)).thenReturn(pendingRequest);

            try {
                leaveService.approveLeave(1L, 2L, "INVALID", "测试");
                fail("应抛出异常");
            } catch (BusinessException e) {
                // 验证不会执行到推进节点的逻辑
                verify(approvalNodeMapper, never()).selectList(any());
            }
        }
    }
}
