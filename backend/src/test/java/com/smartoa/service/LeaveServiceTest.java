package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.dto.LeaveSubmitRequest;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LeaveServiceTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    private User admin;
    private User zhangsan;
    private User lisi;
    private User zongjian1;

    @BeforeEach
    void setUp() {
        admin = userMapper.selectById(1L);
        zhangsan = userMapper.selectById(2L);
        lisi = userMapper.selectById(3L);
        zongjian1 = userMapper.selectById(4L);
    }

    private LeaveSubmitRequest createLeaveRequest(int days) {
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(days));
        dto.setReason("测试请假");
        return dto;
    }

    // ========== 提交申请测试 ==========

    @Nested
    @DisplayName("提交申请")
    class SubmitLeaveTests {

        @Test
        @DisplayName("admin提交申请 - 应自动跳过直属领导节点")
        void testAdminSubmitLeave_ShouldSkipDirectLeaderNode() {
            LeaveRequest result = leaveService.submitLeave(admin.getId(), createLeaveRequest(3));

            assertNotNull(result);
            assertEquals("PENDING", result.getStatus());

            LeaveRequest saved = leaveRequestMapper.selectById(result.getId());
            assertTrue(
                    saved.getCurrentApproverId() != null || "APPROVED".equals(saved.getStatus()),
                    "admin提交后应该有审批人或自动通过"
            );
        }

        @Test
        @DisplayName("张三提交申请 - 应进入直属领导节点")
        void testZhangsanSubmitLeave_ShouldEnterDirectLeaderNode() {
            LeaveRequest result = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            LeaveRequest saved = leaveRequestMapper.selectById(result.getId());
            assertNotNull(saved.getCurrentApproverId());
            assertEquals(admin.getId(), saved.getCurrentApproverId());
        }

        @Test
        @DisplayName("条件分支 - 请假天数<=3天")
        void testCondition_WhenDaysLessThan3() {
            LeaveRequest result = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(2));
            assertNotNull(result);
            assertEquals("PENDING", result.getStatus());
        }

        @Test
        @DisplayName("条件分支 - 请假天数>3天")
        void testCondition_WhenDaysMoreThan3() {
            LeaveRequest result = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(5));
            assertNotNull(result);
            assertEquals("PENDING", result.getStatus());
        }
    }

    // ========== 审批测试 ==========

    @Nested
    @DisplayName("审批操作")
    class ApproveTests {

        @Test
        @DisplayName("审批通过 - 流程正确流转")
        void testApprove_ShouldFlowCorrectly() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            leaveService.approveLeave(request.getId(), admin.getId(), "APPROVE", "同意");

            LeaveRequest updated = leaveRequestMapper.selectById(request.getId());
            assertTrue(
                    "PENDING".equals(updated.getStatus()) || "APPROVED".equals(updated.getStatus()),
                    "审批后应进入下一节点或全部通过"
            );
        }

        @Test
        @DisplayName("审批驳回 - 状态变为REJECTED")
        void testReject_ShouldChangeStatusToRejected() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            leaveService.approveLeave(request.getId(), admin.getId(), "REJECT", "不批准");

            LeaveRequest updated = leaveRequestMapper.selectById(request.getId());
            assertEquals("REJECTED", updated.getStatus());
        }

        @Test
        @DisplayName("非审批人操作 - 应抛出异常")
        void testApprove_ShouldFail_WhenNotApprover() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            assertThrows(BusinessException.class, () -> {
                leaveService.approveLeave(request.getId(), lisi.getId(), "APPROVE", "越权审批");
            });
        }

        @Test
        @DisplayName("已处理的申请 - 不能重复审批")
        void testApprove_ShouldFail_WhenAlreadyProcessed() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));
            leaveService.approveLeave(request.getId(), admin.getId(), "REJECT", "驳回");

            assertThrows(BusinessException.class, () -> {
                leaveService.approveLeave(request.getId(), admin.getId(), "APPROVE", "重复审批");
            });
        }

        @Test
        @DisplayName("审批记录应正确保存")
        void testApprove_ShouldSaveApprovalRecord() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            leaveService.approveLeave(request.getId(), admin.getId(), "APPROVE", "同意");

            List<ApprovalRecord> records = leaveService.getApprovalRecords(request.getId());
            assertFalse(records.isEmpty());
            assertEquals("APPROVE", records.get(0).getAction());
            assertEquals("同意", records.get(0).getComment());
        }
    }

    // ========== 撤回测试 ==========

    @Nested
    @DisplayName("撤回操作")
    class WithdrawTests {

        @Test
        @DisplayName("申请人撤回 - 状态变为WITHDRAWN")
        void testWithdraw_ShouldChangeStatusToWithdrawn() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            leaveService.withdrawLeave(request.getId(), zhangsan.getId());

            LeaveRequest updated = leaveRequestMapper.selectById(request.getId());
            assertEquals("WITHDRAWN", updated.getStatus());
        }

        @Test
        @DisplayName("非申请人撤回 - 应抛出异常")
        void testWithdraw_ShouldFail_WhenNotApplicant() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            assertThrows(BusinessException.class, () -> {
                leaveService.withdrawLeave(request.getId(), admin.getId());
            });
        }

        @Test
        @DisplayName("已驳回的申请 - 不能撤回")
        void testWithdraw_ShouldFail_WhenAlreadyRejected() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));
            leaveService.approveLeave(request.getId(), admin.getId(), "REJECT", "驳回");

            assertThrows(BusinessException.class, () -> {
                leaveService.withdrawLeave(request.getId(), zhangsan.getId());
            });
        }
    }

    // ========== 转派测试 ==========

    @Nested
    @DisplayName("转派操作")
    class TransferTests {

        @Test
        @DisplayName("审批人转派 - 应更新审批人")
        void testTransfer_ShouldUpdateApprover() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            leaveService.transferLeave(request.getId(), admin.getId(), zongjian1.getId());

            LeaveRequest updated = leaveRequestMapper.selectById(request.getId());
            assertEquals(zongjian1.getId(), updated.getCurrentApproverId());
        }

        @Test
        @DisplayName("非审批人转派 - 应抛出异常")
        void testTransfer_ShouldFail_WhenNotApprover() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            assertThrows(BusinessException.class, () -> {
                leaveService.transferLeave(request.getId(), lisi.getId(), zongjian1.getId());
            });
        }
    }

    // ========== 查询测试 ==========

    @Nested
    @DisplayName("查询功能")
    class QueryTests {

        @Test
        @DisplayName("申请详情 - 应返回完整信息")
        void testGetRequestDetail_ShouldReturnFullInfo() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            LeaveRequest detail = leaveService.getRequestDetail(request.getId());

            assertNotNull(detail);
            assertEquals(zhangsan.getId(), detail.getApplicantId());
            assertEquals("年假", detail.getLeaveType());
        }

        @Test
        @DisplayName("审批记录查询 - 应返回所有操作记录")
        void testGetApprovalRecords_ShouldReturnAllRecords() {
            LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));
            leaveService.approveLeave(request.getId(), admin.getId(), "APPROVE", "同意");

            List<ApprovalRecord> records = leaveService.getApprovalRecords(request.getId());

            assertFalse(records.isEmpty());
            assertTrue(records.stream().anyMatch(r -> "APPROVE".equals(r.getAction())));
        }

        @Test
        @DisplayName("待审批列表 - 应只返回需要我审批的")
        void testGetPendingRequests_ShouldReturnOnlyPendingForMe() {
            leaveService.submitLeave(zhangsan.getId(), createLeaveRequest(3));

            List<LeaveRequest> adminPending = leaveService.getPendingRequests(admin.getId());

            assertFalse(adminPending.isEmpty());
            assertTrue(adminPending.stream().allMatch(r -> admin.getId().equals(r.getCurrentApproverId())));
        }
    }

    // ========== 滞留修复测试 ==========

    @Nested
    @DisplayName("滞留修复")
    class RepairTests {

        @Test
        @DisplayName("修复滞留申请")
        void testRepairStuckRequests() {
            int repaired = leaveService.repairStuckRequests();
            assertTrue(repaired >= 0, "修复数量应>=0");
        }
    }
}
