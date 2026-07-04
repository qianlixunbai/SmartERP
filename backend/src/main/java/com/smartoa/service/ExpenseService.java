package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.dto.ExpenseSubmitRequest;
import com.smartoa.entity.*;
import com.smartoa.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRequestMapper expenseRequestMapper;
    private final ExpenseApprovalTaskMapper expenseApprovalTaskMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;
    private final AccountingService accountingService;

    // ========== 提交 ==========

    @Transactional
    public ExpenseRequest submitExpense(Long applicantId, ExpenseSubmitRequest dto) {
        User applicant = userMapper.selectById(applicantId);
        if (applicant == null) {
            throw new BusinessException("用户不存在");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("报销金额必须大于0");
        }

        ExpenseRequest request = new ExpenseRequest();
        request.setApplicantId(applicantId);
        request.setCategory(dto.getCategory());
        request.setAmount(dto.getAmount());
        request.setDescription(dto.getDescription());
        request.setReceiptUrl(dto.getReceiptUrl());
        request.setStatus("PENDING");
        request.setApprovalStep(0);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        expenseRequestMapper.insert(request);

        advanceToNextNode(request, applicant);
        expenseRequestMapper.updateById(request);

        writeAuditLog("SUBMIT", "EXPENSE", request.getId(), applicantId,
                "category=" + dto.getCategory() + ", amount=" + dto.getAmount());

        return request;
    }

    // ========== 审批 ==========

    @Transactional
    public void approveExpense(Long requestId, Long approverId, String action, String comment) {
        ExpenseRequest request = expenseRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("经费申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("该经费申请已处理");
        }

        int currentStep = request.getApprovalStep();

        // 查并行任务
        ExpenseApprovalTask task = null;
        if (request.getCurrentNodeId() != null) {
            task = expenseApprovalTaskMapper.selectOne(
                    new LambdaQueryWrapper<ExpenseApprovalTask>()
                            .eq(ExpenseApprovalTask::getExpenseRequestId, requestId)
                            .eq(ExpenseApprovalTask::getNodeId, request.getCurrentNodeId())
                            .eq(ExpenseApprovalTask::getApproverId, approverId)
                            .eq(ExpenseApprovalTask::getStatus, "PENDING"));
        }

        // 校验审批权限
        if (task == null && !approverId.equals(request.getCurrentApproverId())) {
            throw new BusinessException("您不是当前审批人");
        }

        // 写审计日志
        writeAuditLog("APPROVE", "EXPENSE", requestId, approverId,
                "action=" + action + ", comment=" + comment);

        // 驳回
        if ("REJECT".equals(action)) {
            request.setStatus("REJECTED");
            request.setCurrentApproverId(null);
            request.setCurrentNodeId(null);
            request.setTimeoutTime(null);
            request.setUpdateTime(LocalDateTime.now());
            expenseRequestMapper.updateById(request);

            if (task != null) {
                task.setStatus("COMPLETED");
                task.setUpdateTime(LocalDateTime.now());
                expenseApprovalTaskMapper.updateById(task);
                skipPendingTasks(requestId, request.getCurrentNodeId());
            }
            return;
        }

        // 审批通过 — 处理并行签批逻辑
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setUpdateTime(LocalDateTime.now());
            expenseApprovalTaskMapper.updateById(task);

            ApprovalNode node = approvalNodeMapper.selectById(request.getCurrentNodeId());

            if ("OR_SIGN".equals(node.getSignType())) {
                // 或签：一人通过 → 跳过其余待处理
                skipPendingTasks(requestId, request.getCurrentNodeId());
                request.setApprovalStep(currentStep + 1);
                User applicant = userMapper.selectById(request.getApplicantId());
                boolean hasNext = advanceToNextNode(request, applicant);
                if (!hasNext) {
                    finalizeApproval(request);
                }
                expenseRequestMapper.updateById(request);
            } else {
                // 会签：全部通过才推进
                Long pendingCount = expenseApprovalTaskMapper.selectCount(
                        new LambdaQueryWrapper<ExpenseApprovalTask>()
                                .eq(ExpenseApprovalTask::getExpenseRequestId, requestId)
                                .eq(ExpenseApprovalTask::getNodeId, request.getCurrentNodeId())
                                .eq(ExpenseApprovalTask::getStatus, "PENDING"));
                if (pendingCount == 0) {
                    request.setApprovalStep(currentStep + 1);
                    User applicant = userMapper.selectById(request.getApplicantId());
                    boolean hasNext = advanceToNextNode(request, applicant);
                    if (!hasNext) {
                        finalizeApproval(request);
                    }
                    expenseRequestMapper.updateById(request);
                }
            }
        } else {
            // 单人审批
            request.setApprovalStep(currentStep + 1);
            User applicant = userMapper.selectById(request.getApplicantId());
            boolean hasNext = advanceToNextNode(request, applicant);
            if (!hasNext) {
                finalizeApproval(request);
            }
            expenseRequestMapper.updateById(request);
        }
    }

    /**
     * 审批全部通过后：自动入账
     */
    private void finalizeApproval(ExpenseRequest request) {
        request.setStatus("APPROVED");
        request.setCurrentApproverId(null);
        request.setCurrentNodeId(null);
        request.setTimeoutTime(null);

        // 自动调用复式记账入账
        try {
            String txnId = accountingService.post(
                    request.getId(),
                    request.getCategory(),
                    request.getAmount(),
                    request.getApplicantId(),
                    "经费报销自动入账 #" + request.getId());
            request.setTransactionId(txnId);
            request.setStatus("POSTED");
            log.info("经费申请#{} 审批通过，自动入账 txnId={}", request.getId(), txnId);
        } catch (Exception e) {
            log.error("经费申请#{} 入账失败: {}", request.getId(), e.getMessage());
            // 入账失败仍保持APPROVED状态，后续可手动入账
        }
    }

    // ========== 撤回 ==========

    @Transactional
    public void withdrawExpense(Long requestId, Long applicantId) {
        ExpenseRequest request = expenseRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("经费申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("只能撤回审批中的申请");
        }
        if (!applicantId.equals(request.getApplicantId())) {
            throw new BusinessException("只能撤回自己的申请");
        }

        if (request.getCurrentNodeId() != null) {
            skipPendingTasks(requestId, request.getCurrentNodeId());
        }

        writeAuditLog("WITHDRAW", "EXPENSE", requestId, applicantId, null);

        request.setStatus("WITHDRAWN");
        request.setCurrentApproverId(null);
        request.setCurrentNodeId(null);
        request.setTimeoutTime(null);
        request.setUpdateTime(LocalDateTime.now());
        expenseRequestMapper.updateById(request);
    }

    // ========== 冲销（已入账的报销） ==========

    @Transactional
    public void reverseExpense(Long requestId, Long operatorId, String reason) {
        ExpenseRequest request = expenseRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("经费申请不存在");
        }
        if (!"POSTED".equals(request.getStatus())) {
            throw new BusinessException("只能冲销已入账的申请");
        }
        if (request.getTransactionId() == null) {
            throw new BusinessException("该申请无关联分录");
        }

        accountingService.reverse(request.getTransactionId(), operatorId, reason);

        request.setStatus("REVERSED");
        request.setUpdateTime(LocalDateTime.now());
        expenseRequestMapper.updateById(request);

        writeAuditLog("REVERSE", "EXPENSE", requestId, operatorId, "reason=" + reason);
    }

    // ========== 审批流引擎（复用 approval_node 表） ==========

    private boolean advanceToNextNode(ExpenseRequest request, User applicant) {
        // 经费报销使用"经费报销"模板（ID=1 或按名称查找）
        Long templateId = findExpenseTemplateId();
        if (templateId == null) {
            log.warn("未找到经费报销审批模板，跳过审批流");
            return false;
        }

        List<ApprovalNode> nodes = approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getTemplateId, templateId)
                        .orderByAsc(ApprovalNode::getSortOrder));

        if (nodes.isEmpty()) {
            return false;
        }

        Long currentNodeId = request.getCurrentNodeId();
        int startIndex = 0;
        if (currentNodeId != null) {
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).getId().equals(currentNodeId)) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        for (int i = startIndex; i < nodes.size(); i++) {
            ApprovalNode node = nodes.get(i);
            if (evaluateCondition(node.getConditionExpression(), request)) {
                List<Long> approverIds = resolveApprovers(node, applicant);

                if (approverIds == null || approverIds.isEmpty()) {
                    log.info("跳过节点【{}】（无可用审批人）", node.getNodeName());
                    continue;
                }

                request.setCurrentNodeId(node.getId());

                if (isParallel(node)) {
                    request.setCurrentApproverId(null);
                    LocalDateTime now = LocalDateTime.now();
                    for (Long aid : approverIds) {
                        ExpenseApprovalTask task = new ExpenseApprovalTask();
                        task.setExpenseRequestId(request.getId());
                        task.setNodeId(node.getId());
                        task.setApproverId(aid);
                        task.setStatus("PENDING");
                        task.setCreateTime(now);
                        task.setUpdateTime(now);
                        expenseApprovalTaskMapper.insert(task);
                    }
                } else {
                    request.setCurrentApproverId(approverIds.get(0));
                }

                if (node.getTimeoutHours() != null && node.getTimeoutHours() > 0) {
                    request.setTimeoutTime(LocalDateTime.now().plusHours(node.getTimeoutHours()));
                } else {
                    request.setTimeoutTime(null);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 查找经费报销的审批模板ID
     * 优先按名称"经费报销"查找，找不到则用templateId=1
     */
    private Long findExpenseTemplateId() {
        // 简单实现：使用templateId=1（跟请假共用同一套审批模板）
        // 后续可创建独立的经费报销模板
        return 1L;
    }

    private boolean isParallel(ApprovalNode node) {
        return "COUNTER_SIGN".equals(node.getSignType()) || "OR_SIGN".equals(node.getSignType());
    }

    /**
     * SpEL条件求值 — 经费版本，暴露 amount 和 category
     */
    public record ExpenseConditionVars(BigDecimal amount, String category) {}

    private boolean evaluateCondition(String expression, ExpenseRequest request) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        try {
            ExpenseConditionVars vars = new ExpenseConditionVars(
                    request.getAmount(), request.getCategory());
            StandardEvaluationContext ctx = new StandardEvaluationContext(vars);
            Boolean result = new SpelExpressionParser()
                    .parseExpression(expression).getValue(ctx, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("SpEL求值失败: expr='{}' — {}", expression, e.getMessage());
            return true;
        }
    }

    private List<Long> resolveApprovers(ApprovalNode node, User applicant) {
        if (!isParallel(node)) {
            Long singleId = switch (node.getApproverType()) {
                case "DIRECT_LEADER" -> applicant.getDirectLeaderId();
                case "DEPARTMENT_HEAD" -> applicant.getDepartmentHeadId();
                case "SPECIFIC_USER" -> node.getApproverId();
                default -> throw new BusinessException("不支持的审批人类型: " + node.getApproverType());
            };
            if (singleId == null) {
                log.info("节点【{}】审批人为空（{}），自动跳过", node.getNodeName(), node.getApproverType());
                return null;
            }
            return List.of(singleId);
        }

        if (node.getApproverIds() == null || node.getApproverIds().isBlank()) {
            log.info("并行签批节点【{}】无审批人配置，自动跳过", node.getNodeName());
            return null;
        }
        return Arrays.stream(node.getApproverIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private void skipPendingTasks(Long requestId, Long nodeId) {
        if (nodeId != null) {
            expenseApprovalTaskMapper.update(null,
                    new LambdaUpdateWrapper<ExpenseApprovalTask>()
                            .eq(ExpenseApprovalTask::getExpenseRequestId, requestId)
                            .eq(ExpenseApprovalTask::getNodeId, nodeId)
                            .eq(ExpenseApprovalTask::getStatus, "PENDING")
                            .set(ExpenseApprovalTask::getStatus, "SKIPPED"));
        }
    }

    // ========== 查询 ==========

    public List<ExpenseRequest> getAllExpenses() {
        return expenseRequestMapper.selectList(new LambdaQueryWrapper<ExpenseRequest>()
                .orderByDesc(ExpenseRequest::getCreateTime));
    }

    public List<ExpenseRequest> getMyExpenses(Long applicantId) {
        return expenseRequestMapper.selectList(new LambdaQueryWrapper<ExpenseRequest>()
                .eq(ExpenseRequest::getApplicantId, applicantId)
                .orderByDesc(ExpenseRequest::getCreateTime));
    }

    public List<ExpenseRequest> getPendingExpenses(Long approverId) {
        List<ExpenseRequest> result = new ArrayList<>(expenseRequestMapper.selectList(
                new LambdaQueryWrapper<ExpenseRequest>()
                        .eq(ExpenseRequest::getCurrentApproverId, approverId)
                        .eq(ExpenseRequest::getStatus, "PENDING")));

        List<ExpenseApprovalTask> tasks = expenseApprovalTaskMapper.selectList(
                new LambdaQueryWrapper<ExpenseApprovalTask>()
                        .eq(ExpenseApprovalTask::getApproverId, approverId)
                        .eq(ExpenseApprovalTask::getStatus, "PENDING"));

        if (!tasks.isEmpty()) {
            Set<Long> existingIds = result.stream().map(ExpenseRequest::getId).collect(Collectors.toSet());
            Set<Long> parallelIds = tasks.stream().map(ExpenseApprovalTask::getExpenseRequestId).collect(Collectors.toSet());
            parallelIds.removeAll(existingIds);
            if (!parallelIds.isEmpty()) {
                result.addAll(expenseRequestMapper.selectBatchIds(parallelIds));
            }
        }

        result.sort(Comparator.comparing(ExpenseRequest::getCreateTime).reversed());
        return result;
    }

    public ExpenseRequest getExpenseDetail(Long requestId) {
        return expenseRequestMapper.selectById(requestId);
    }

    public List<AuditLog> getAuditLogs(Long requestId) {
        return auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getTargetType, "EXPENSE")
                .eq(AuditLog::getTargetId, requestId)
                .orderByAsc(AuditLog::getCreateTime));
    }

    public List<ExpenseApprovalTask> getApprovalTasks(Long requestId) {
        return expenseApprovalTaskMapper.selectList(new LambdaQueryWrapper<ExpenseApprovalTask>()
                .eq(ExpenseApprovalTask::getExpenseRequestId, requestId)
                .eq(ExpenseApprovalTask::getStatus, "PENDING"));
    }

    private void writeAuditLog(String action, String targetType, Long targetId,
                               Long actorId, String detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setActorId(actorId);
        auditLog.setDetail(detail);
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }
}
