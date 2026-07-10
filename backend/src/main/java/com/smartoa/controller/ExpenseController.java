package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.dto.ExpenseApproveRequest;
import com.smartoa.dto.ExpenseReverseRequest;
import com.smartoa.dto.ExpenseSubmitRequest;
import com.smartoa.entity.*;
import com.smartoa.service.AccountingService;
import com.smartoa.service.ApprovalAuthorizationService;
import com.smartoa.service.ExpenseService;
import com.smartoa.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
public class ExpenseController {

    private final ExpenseService expenseService;
    private final AccountingService accountingService;
    private final UserService userService;
    private final ApprovalAuthorizationService approvalAuthorizationService;

    // ========== 提交经费申请 ==========

    @PostMapping("/api/expense/submit")
    public Result<ExpenseRequest> submit(@RequestBody @Valid ExpenseSubmitRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(expenseService.submitExpense(user.getId(), dto));
    }

    // ========== 审批经费申请 ==========

    @PostMapping("/api/expense/approve")
    public Result<Void> approve(@RequestBody @Valid ExpenseApproveRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        expenseService.approveExpense(dto.getRequestId(), user.getId(), dto.getAction(), dto.getComment());
        return Result.success(null, "操作成功");
    }

    // ========== 撤回 ==========

    @PostMapping("/api/expense/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable @Positive(message = "ID必须为正整数") Long id) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        expenseService.withdrawExpense(id, user.getId());
        return Result.success(null, "撤回成功");
    }

    // ========== 冲销（仅管理员） ==========

    @PostMapping("/api/expense/{id}/reverse")
    public Result<Void> reverse(@PathVariable @Positive(message = "ID必须为正整数") Long id,
                                @RequestBody @Valid ExpenseReverseRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        expenseService.reverseExpense(id, user.getId(), dto.getReason());
        return Result.success(null, "冲销成功");
    }

    // ========== 我的经费申请 ==========

    @GetMapping("/api/expense/my-expenses")
    public Result<List<ExpenseRequest>> myExpenses() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(expenseService.getMyExpenses(user.getId()));
    }

    // ========== 待我审批 ==========

    @GetMapping("/api/expense/pending")
    public Result<List<ExpenseRequest>> pending() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(expenseService.getPendingExpenses(user.getId()));
    }

    // ========== 全部经费申请（管理员） ==========

    @GetMapping("/api/expense/all")
    public Result<List<ExpenseRequest>> all() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        return Result.success(expenseService.getAllExpenses());
    }

    // ========== 详情 ==========

    @GetMapping("/api/expense/{id}")
    public Result<ExpenseRequest> detail(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        approvalAuthorizationService.requireReadableExpense(id, user);
        return Result.success(expenseService.getExpenseDetail(id));
    }

    // ========== 审计日志 ==========

    @GetMapping("/api/expense/{id}/audit-logs")
    public Result<List<AuditLog>> auditLogs(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        approvalAuthorizationService.requireReadableExpense(id, user);
        return Result.success(expenseService.getAuditLogs(id));
    }

    // ========== 并行审批任务 ==========

    @GetMapping("/api/expense/{id}/tasks")
    public Result<List<ExpenseApprovalTask>> tasks(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        approvalAuthorizationService.requireReadableExpense(id, user);
        return Result.success(expenseService.getApprovalTasks(id));
    }

    // ========== 试算平衡（管理员） ==========

    @GetMapping("/api/accounting/trial-balance")
    public Result<Map<String, Object>> trialBalance() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        return Result.success(accountingService.trialBalance());
    }

    // ========== 科目余额汇总（管理员） ==========

    @GetMapping("/api/accounting/balances")
    public Result<List<Map<String, Object>>> balances() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        return Result.success(accountingService.accountBalances());
    }
}
