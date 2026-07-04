package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.dto.ExpenseSubmitRequest;
import com.smartoa.entity.ExpenseApprovalTask;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.mapper.AuditLogMapper;
import com.smartoa.mapper.ExpenseApprovalTaskMapper;
import com.smartoa.mapper.ExpenseRequestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExpenseServiceTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRequestMapper expenseRequestMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private ExpenseApprovalTaskMapper expenseApprovalTaskMapper;

    // 测试用户（与数据库seed一致）：
    // admin(1) = zongjian，zhangsan(2) = manager，lisi(3) = employee

    private ExpenseSubmitRequest buildDto(String category, BigDecimal amount, String desc) {
        ExpenseSubmitRequest dto = new ExpenseSubmitRequest();
        dto.setCategory(category);
        dto.setAmount(amount);
        dto.setDescription(desc);
        return dto;
    }

    // ========== 提交测试 ==========

    @Nested
    @DisplayName("提交报销（submitExpense）")
    class SubmitTests {

        @Test
        @DisplayName("正常提交 - 应创建PENDING状态的报销单")
        void testSubmit_ShouldCreatePendingExpense() {
            ExpenseSubmitRequest dto = buildDto("办公", new BigDecimal("500.00"), "购买办公用品");
            ExpenseRequest expense = expenseService.submitExpense(1L, dto);

            assertNotNull(expense.getId());
            assertEquals("PENDING", expense.getStatus());
            assertEquals(0, new BigDecimal("500.00").compareTo(expense.getAmount()));
        }

        @Test
        @DisplayName("零金额提交 - 应抛出异常")
        void testSubmit_ZeroAmount_ShouldThrow() {
            ExpenseSubmitRequest dto = buildDto("办公", BigDecimal.ZERO, "零金额");
            assertThrows(BusinessException.class, () ->
                    expenseService.submitExpense(1L, dto));
        }

        @Test
        @DisplayName("负金额提交 - 应抛出异常")
        void testSubmit_NegativeAmount_ShouldThrow() {
            ExpenseSubmitRequest dto = buildDto("办公", new BigDecimal("-100"), "负金额");
            assertThrows(BusinessException.class, () ->
                    expenseService.submitExpense(1L, dto));
        }
    }

    // ========== 撤回测试 ==========

    @Nested
    @DisplayName("撤回报销（withdrawExpense）")
    class WithdrawTests {

        @Test
        @DisplayName("正常撤回 - PENDING状态应可撤回")
        void testWithdraw_Pending_ShouldSucceed() {
            ExpenseSubmitRequest dto = buildDto("办公", new BigDecimal("200"), "可撤回");
            ExpenseRequest expense = expenseService.submitExpense(1L, dto);

            expenseService.withdrawExpense(expense.getId(), 1L);

            ExpenseRequest updated = expenseRequestMapper.selectById(expense.getId());
            assertEquals("WITHDRAWN", updated.getStatus());
        }

        @Test
        @DisplayName("非本人撤回 - 应抛出异常")
        void testWithdraw_WrongUser_ShouldThrow() {
            ExpenseSubmitRequest dto = buildDto("办公", new BigDecimal("200"), "测试");
            ExpenseRequest expense = expenseService.submitExpense(1L, dto);

            assertThrows(BusinessException.class, () ->
                    expenseService.withdrawExpense(expense.getId(), 2L));
        }
    }

    // ========== 驳回测试 ==========

    @Nested
    @DisplayName("驳回报销（rejectExpense）")
    class RejectTests {

        @Test
        @DisplayName("正常驳回 - 应设为REJECTED状态")
        void testReject_ShouldSetRejected() {
            // 使用 zhangsan(2) 提交，其 direct_leader=1(admin)，部门主管=4
            // 模板第一个节点 DIRECT_LEADER → 审批人=admin(1)
            ExpenseSubmitRequest dto = buildDto("办公", new BigDecimal("300"), "待驳回");
            ExpenseRequest expense = expenseService.submitExpense(2L, dto);

            ExpenseRequest submitted = expenseRequestMapper.selectById(expense.getId());
            assertEquals("PENDING", submitted.getStatus());
            assertNotNull(submitted.getCurrentApproverId(), "应有当前审批人");

            // 用当前审批人驳回
            expenseService.approveExpense(expense.getId(), submitted.getCurrentApproverId(), "REJECT", "不予批准");

            ExpenseRequest updated = expenseRequestMapper.selectById(expense.getId());
            assertEquals("REJECTED", updated.getStatus());
        }
    }
}
