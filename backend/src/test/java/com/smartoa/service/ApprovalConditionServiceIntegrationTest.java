package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.dto.ExpenseSubmitRequest;
import com.smartoa.dto.LeaveSubmitRequest;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalConditionServiceIntegrationTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private ApprovalNodeMapper approvalNodeMapper;
    @Mock
    private ApprovalTaskMapper approvalTaskMapper;
    @Mock
    private ExpenseRequestMapper expenseRequestMapper;
    @Mock
    private ExpenseApprovalTaskMapper expenseApprovalTaskMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AccountingService accountingService;

    private LeaveService leaveService;
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        ApprovalConditionEvaluator evaluator = new ApprovalConditionEvaluator();
        leaveService = new LeaveService(leaveRequestMapper, approvalRecordMapper, approvalNodeMapper,
                approvalTaskMapper, userMapper, evaluator);
        expenseService = new ExpenseService(expenseRequestMapper, expenseApprovalTaskMapper, approvalNodeMapper,
                userMapper, auditLogMapper, accountingService, evaluator);
    }

    @Test
    void submitLeave_WithInvalidCondition_ShouldPropagate422BeforeTaskOrFinalUpdate() {
        User applicant = new User();
        applicant.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(applicant);
        when(approvalNodeMapper.selectList(any())).thenReturn(List.of(node("days = 1")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> leaveService.submitLeave(1L, leaveSubmitRequest()));

        assertInvalidCondition(exception);
        verifyNoInteractions(approvalTaskMapper);
        verify(leaveRequestMapper, never()).updateById(any(LeaveRequest.class));
        verify(userMapper, times(1)).selectById(1L);
    }

    @Test
    void submitExpense_WithInvalidCondition_ShouldPropagate422BeforeTaskOrFinalUpdate() {
        User applicant = new User();
        applicant.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(applicant);
        when(approvalNodeMapper.selectList(any())).thenReturn(List.of(node("amount.toString()")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> expenseService.submitExpense(1L, expenseSubmitRequest()));

        assertInvalidCondition(exception);
        verifyNoInteractions(expenseApprovalTaskMapper, auditLogMapper, accountingService);
        verify(expenseRequestMapper, never()).updateById(any(ExpenseRequest.class));
        verify(userMapper, times(1)).selectById(1L);
    }

    private ApprovalNode node(String expression) {
        ApprovalNode node = new ApprovalNode();
        node.setId(10L);
        node.setConditionExpression(expression);
        node.setApproverType("SPECIFIC_USER");
        node.setApproverId(2L);
        return node;
    }

    private LeaveSubmitRequest leaveSubmitRequest() {
        LeaveSubmitRequest request = new LeaveSubmitRequest();
        request.setTemplateId(1L);
        request.setLeaveType("年假");
        request.setStartDate(LocalDate.of(2026, 1, 1));
        request.setEndDate(LocalDate.of(2026, 1, 3));
        return request;
    }

    private ExpenseSubmitRequest expenseSubmitRequest() {
        ExpenseSubmitRequest request = new ExpenseSubmitRequest();
        request.setCategory("差旅");
        request.setAmount(new BigDecimal("1000.00"));
        return request;
    }

    private void assertInvalidCondition(BusinessException exception) {
        assertEquals(422, exception.getCode());
        assertEquals("审批条件配置无效，请联系管理员", exception.getMessage());
    }
}
