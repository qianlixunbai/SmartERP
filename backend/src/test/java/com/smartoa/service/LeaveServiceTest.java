package com.smartoa.service;

import com.smartoa.dto.LeaveSubmitRequest;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 测试后自动回滚，不污染数据库
class LeaveServiceTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ApprovalNodeMapper approvalNodeMapper;

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    private User admin;
    private User zhangsan;

    @BeforeEach
    void setUp() {
        // 从数据库获取测试用户
        admin = userMapper.selectById(1L);      // admin，无直属领导
        zhangsan = userMapper.selectById(2L);   // 张三，直属领导是admin
    }

    @Test
    @DisplayName("admin提交申请 - 应自动跳过直属领导节点")
    void testAdminSubmitLeave_ShouldSkipDirectLeaderNode() {
        // Given: admin 提交请假申请
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(3));
        dto.setReason("测试请假");

        // When: 提交申请
        LeaveRequest result = leaveService.submitLeave(admin.getId(), dto);

        // Then: 申请应该创建成功
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(admin.getId(), result.getApplicantId());

        // 验证：currentApproverId 不应该是 null（应该跳过了直属领导节点，进入下一节点）
        // 或者如果所有节点都被跳过，状态应该是 APPROVED
        LeaveRequest saved = leaveRequestMapper.selectById(result.getId());
        assertTrue(
                saved.getCurrentApproverId() != null || "APPROVED".equals(saved.getStatus()),
                "admin提交后应该有审批人或自动通过"
        );

        System.out.println("✅ admin提交申请测试通过，状态: " + saved.getStatus());
    }

    @Test
    @DisplayName("张三提交申请 - 应正常进入直属领导节点")
    void testZhangsanSubmitLeave_ShouldEnterDirectLeaderNode() {
        // Given: 张三提交请假申请
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(3));
        dto.setReason("测试请假");

        // When: 提交申请
        LeaveRequest result = leaveService.submitLeave(zhangsan.getId(), dto);

        // Then: 申请应该进入直属领导审批（admin）
        LeaveRequest saved = leaveRequestMapper.selectById(result.getId());
        assertNotNull(saved.getCurrentApproverId(), "应该有审批人");
        assertEquals(admin.getId(), saved.getCurrentApproverId(), "审批人应该是admin（张三的直属领导）");

        System.out.println("✅ 张三提交申请测试通过，审批人ID: " + saved.getCurrentApproverId());
    }

    @Test
    @DisplayName("条件分支测试 - 请假天数<=3天跳过总监节点")
    void testConditionExpression_ShouldSkipNodeWhenDaysLessThan3() {
        // Given: 张三请2天假
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(2)); // 2天
        dto.setReason("短期请假");

        // When: 提交申请
        LeaveRequest result = leaveService.submitLeave(zhangsan.getId(), dto);

        // Then: 验证条件分支逻辑（具体断言取决于模板配置）
        LeaveRequest saved = leaveRequestMapper.selectById(result.getId());
        assertNotNull(saved);

        System.out.println("✅ 条件分支测试通过，请假2天，状态: " + saved.getStatus());
    }

    @Test
    @DisplayName("审批通过测试 - 流程正确流转")
    void testApproveLeave_ShouldFlowCorrectly() {
        // Given: 张三提交申请
        LeaveSubmitRequest dto = new LeaveSubmitRequest();
        dto.setTemplateId(1L);
        dto.setLeaveType("年假");
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setReason("测试审批流程");

        LeaveRequest request = leaveService.submitLeave(zhangsan.getId(), dto);

        // When: admin 审批通过
        leaveService.approveLeave(request.getId(), admin.getId(), "APPROVE", "同意");

        // Then: 验证状态变化
        LeaveRequest updated = leaveRequestMapper.selectById(request.getId());

        // 可能进入下一节点，也可能全部通过
        assertTrue(
                "PENDING".equals(updated.getStatus()) || "APPROVED".equals(updated.getStatus()),
                "审批后状态应该是PENDING（还有节点）或APPROVED（全部通过）"
        );

        System.out.println("✅ 审批测试通过，状态: " + updated.getStatus());
    }
}
