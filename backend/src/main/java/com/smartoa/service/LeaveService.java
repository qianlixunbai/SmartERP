package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.dto.LeaveSubmitRequest;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.ApprovalNodeMapper;
import com.smartoa.mapper.ApprovalRecordMapper;
import com.smartoa.mapper.ApprovalTaskMapper;
import com.smartoa.mapper.LeaveRequestMapper;
import com.smartoa.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final UserMapper userMapper;

    @Transactional
    public LeaveRequest submitLeave(Long applicantId, LeaveSubmitRequest dto) {
        User applicant = userMapper.selectById(applicantId);
        if (applicant == null) {
            throw new BusinessException("用户不存在");
        }

        LeaveRequest request = new LeaveRequest();
        request.setApplicantId(applicantId);
        request.setTemplateId(dto.getTemplateId());
        request.setLeaveType(dto.getLeaveType());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setReason(dto.getReason());
        request.setStatus("PENDING");
        request.setApprovalStep(0);
        leaveRequestMapper.insert(request);

        advanceToNextNode(request, applicant);
        leaveRequestMapper.updateById(request);
        return request;
    }

    @Transactional
    public void approveLeave(Long requestId, Long approverId, String action, String comment) {
        LeaveRequest request = leaveRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("请假单不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("该请假单已处理");
        }

        int currentStep = request.getApprovalStep();

        ApprovalTask task = null;
        if (request.getCurrentNodeId() != null) {
            task = approvalTaskMapper.selectOne(
                    new LambdaQueryWrapper<ApprovalTask>()
                            .eq(ApprovalTask::getLeaveRequestId, requestId)
                            .eq(ApprovalTask::getNodeId, request.getCurrentNodeId())
                            .eq(ApprovalTask::getApproverId, approverId)
                            .eq(ApprovalTask::getStatus, "PENDING"));
        }

        if (task == null && !approverId.equals(request.getCurrentApproverId())) {
            throw new BusinessException("您不是当前审批人");
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setLeaveRequestId(requestId);
        record.setApproverId(approverId);
        record.setAction(action);
        record.setComment(comment);
        record.setApprovalStep(currentStep);
        record.setNodeId(request.getCurrentNodeId());
        approvalRecordMapper.insert(record);

        if ("REJECT".equals(action)) {
            request.setStatus("REJECTED");
            request.setCurrentApproverId(null);
            request.setCurrentNodeId(null);
            request.setTimeoutTime(null);
            leaveRequestMapper.updateById(request);
            if (task != null) {
                task.setStatus("COMPLETED");
                approvalTaskMapper.updateById(task);
                // 跳过该节点其余待处理任务
                approvalTaskMapper.update(null,
                        new LambdaUpdateWrapper<ApprovalTask>()
                                .eq(ApprovalTask::getLeaveRequestId, requestId)
                                .eq(ApprovalTask::getNodeId, request.getCurrentNodeId())
                                .eq(ApprovalTask::getStatus, "PENDING")
                                .set(ApprovalTask::getStatus, "SKIPPED"));
            }
            return;
        }

        if (task != null) {
            task.setStatus("COMPLETED");
            approvalTaskMapper.updateById(task);

            ApprovalNode node = approvalNodeMapper.selectById(request.getCurrentNodeId());

            if ("OR_SIGN".equals(node.getSignType())) {
                approvalTaskMapper.update(null,
                        new LambdaUpdateWrapper<ApprovalTask>()
                                .eq(ApprovalTask::getLeaveRequestId, requestId)
                                .eq(ApprovalTask::getNodeId, request.getCurrentNodeId())
                                .eq(ApprovalTask::getStatus, "PENDING")
                                .set(ApprovalTask::getStatus, "SKIPPED"));
                request.setApprovalStep(currentStep + 1);
                User applicant = userMapper.selectById(request.getApplicantId());
                boolean hasNext = advanceToNextNode(request, applicant);
                if (!hasNext) {
                    request.setStatus("APPROVED");
                    request.setCurrentApproverId(null);
                    request.setCurrentNodeId(null);
                    request.setTimeoutTime(null);
                }
                leaveRequestMapper.updateById(request);
            } else {
                Long pendingCount = approvalTaskMapper.selectCount(
                        new LambdaQueryWrapper<ApprovalTask>()
                                .eq(ApprovalTask::getLeaveRequestId, requestId)
                                .eq(ApprovalTask::getNodeId, request.getCurrentNodeId())
                                .eq(ApprovalTask::getStatus, "PENDING"));
                if (pendingCount == 0) {
                    request.setApprovalStep(currentStep + 1);
                    User applicant = userMapper.selectById(request.getApplicantId());
                    boolean hasNext = advanceToNextNode(request, applicant);
                    if (!hasNext) {
                        request.setStatus("APPROVED");
                        request.setCurrentApproverId(null);
                        request.setCurrentNodeId(null);
                        request.setTimeoutTime(null);
                    }
                    leaveRequestMapper.updateById(request);
                }
            }
        } else {
            request.setApprovalStep(currentStep + 1);
            User applicant = userMapper.selectById(request.getApplicantId());
            boolean hasNext = advanceToNextNode(request, applicant);
            if (!hasNext) {
                request.setStatus("APPROVED");
                request.setCurrentApproverId(null);
                request.setCurrentNodeId(null);
                request.setTimeoutTime(null);
            }
            leaveRequestMapper.updateById(request);
        }
    }

    /**
     * 查找当前节点之后第一个满足条件的节点并解析审批人。
     * SINGLE 模式设 currentApproverId，并行模式创建 approval_task 并设 currentApproverId=null。
     */
    private boolean advanceToNextNode(LeaveRequest request, User applicant) {
        Long currentNodeId = request.getCurrentNodeId();
        Long templateId = request.getTemplateId();

        List<ApprovalNode> nodes = approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getTemplateId, templateId)
                        .orderByAsc(ApprovalNode::getSortOrder));

        if (nodes.isEmpty()) {
            return false;
        }

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
                request.setCurrentNodeId(node.getId());

                if (isParallel(node)) {
                    request.setCurrentApproverId(null);
                    for (Long aid : approverIds) {
                        ApprovalTask task = new ApprovalTask();
                        task.setLeaveRequestId(request.getId());
                        task.setNodeId(node.getId());
                        task.setApproverId(aid);
                        task.setStatus("PENDING");
                        approvalTaskMapper.insert(task);
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

    private boolean isParallel(ApprovalNode node) {
        return "COUNTER_SIGN".equals(node.getSignType()) || "OR_SIGN".equals(node.getSignType());
    }

    public record ConditionVars(String leaveType, long days,
                                 java.time.LocalDate startDate, java.time.LocalDate endDate) {}

    private boolean evaluateCondition(String expression, LeaveRequest request) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        try {
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            ConditionVars vars = new ConditionVars(
                    request.getLeaveType(), days,
                    request.getStartDate(), request.getEndDate());
            StandardEvaluationContext ctx = new StandardEvaluationContext(vars);
            Boolean result = new SpelExpressionParser()
                    .parseExpression(expression).getValue(ctx, Boolean.class);
            log.debug("condition eval: expr='{}' days={} leaveType={} => {}", expression, days, request.getLeaveType(), result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("condition eval error: expr='{}' — {}", expression, e.getMessage());
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
                throw new BusinessException("节点【" + node.getNodeName() + "】无法确定审批人");
            }
            return List.of(singleId);
        }

        if (node.getApproverIds() == null || node.getApproverIds().isBlank()) {
            throw new BusinessException("并行签批节点【" + node.getNodeName() + "】缺少审批人配置");
        }
        return Arrays.stream(node.getApproverIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    // ========== 撤回 ==========

    @Transactional
    public void withdrawLeave(Long requestId, Long applicantId) {
        LeaveRequest request = leaveRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("请假单不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("只能撤回审批中的申请");
        }
        if (!applicantId.equals(request.getApplicantId())) {
            throw new BusinessException("只能撤回自己的申请");
        }

        // 跳过当前节点的待处理并行任务
        if (request.getCurrentNodeId() != null) {
            approvalTaskMapper.update(null,
                    new LambdaUpdateWrapper<ApprovalTask>()
                            .eq(ApprovalTask::getLeaveRequestId, requestId)
                            .eq(ApprovalTask::getNodeId, request.getCurrentNodeId())
                            .eq(ApprovalTask::getStatus, "PENDING")
                            .set(ApprovalTask::getStatus, "SKIPPED"));
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setLeaveRequestId(requestId);
        record.setApproverId(applicantId);
        record.setAction("WITHDRAW");
        record.setApprovalStep(request.getApprovalStep());
        record.setNodeId(request.getCurrentNodeId());
        approvalRecordMapper.insert(record);

        request.setStatus("WITHDRAWN");
        request.setCurrentApproverId(null);
        request.setCurrentNodeId(null);
        request.setTimeoutTime(null);
        leaveRequestMapper.updateById(request);
    }

    // ========== 转派 ==========

    @Transactional
    public void transferLeave(Long requestId, Long currentApproverId, Long toUserId) {
        LeaveRequest request = leaveRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("请假单不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("只能转派审批中的申请");
        }
        if (request.getCurrentApproverId() == null) {
            throw new BusinessException("并行审批节点不支持转派");
        }
        if (!currentApproverId.equals(request.getCurrentApproverId())) {
            throw new BusinessException("您不是当前审批人，无法转派");
        }

        User target = userMapper.selectById(toUserId);
        if (target == null) {
            throw new BusinessException("目标用户不存在");
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setLeaveRequestId(requestId);
        record.setApproverId(currentApproverId);
        record.setAction("TRANSFER");
        record.setComment("转派给 " + target.getRealName());
        record.setApprovalStep(request.getApprovalStep());
        record.setNodeId(request.getCurrentNodeId());
        approvalRecordMapper.insert(record);

        request.setCurrentApproverId(toUserId);
        leaveRequestMapper.updateById(request);
    }

    // ========== 滞留修復 ==========

    @Transactional
    public int repairStuckRequests() {
        List<LeaveRequest> stuck = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getStatus, "PENDING")
                        .isNull(LeaveRequest::getCurrentApproverId));

        int repaired = 0;
        for (LeaveRequest r : stuck) {
            // 跳过有关联 PENDING 任务的（并行节点正常状态）
            Long taskCount = approvalTaskMapper.selectCount(
                    new LambdaQueryWrapper<ApprovalTask>()
                            .eq(ApprovalTask::getLeaveRequestId, r.getId())
                            .eq(ApprovalTask::getStatus, "PENDING"));
            if (taskCount > 0) continue;

            User applicant = userMapper.selectById(r.getApplicantId());
            if (applicant == null) continue;
            r.setApprovalStep(0);
            r.setCurrentNodeId(null);
            advanceToNextNode(r, applicant);
            leaveRequestMapper.updateById(r);
            repaired++;
        }
        return repaired;
    }

    // ========== 超时自动升级 ==========

    @Transactional
    public int checkTimeouts() {
        List<LeaveRequest> timedOut = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getStatus, "PENDING")
                        .isNotNull(LeaveRequest::getTimeoutTime)
                        .le(LeaveRequest::getTimeoutTime, LocalDateTime.now()));

        int count = 0;
        for (LeaveRequest r : timedOut) {
            try {
                processTimeout(r);
                count++;
            } catch (Exception e) {
                log.warn("timeout escalation failed for request {}: {}", r.getId(), e.getMessage());
            }
        }
        return count;
    }

    private void processTimeout(LeaveRequest request) {
        ApprovalNode node = approvalNodeMapper.selectById(request.getCurrentNodeId());
        if (node == null || node.getTimeoutHours() == null) return;

        String action = node.getTimeoutAction() != null ? node.getTimeoutAction() : "ESCALATE";

        ApprovalRecord record = new ApprovalRecord();
        record.setLeaveRequestId(request.getId());
        record.setApproverId(0L);
        record.setAction("TIMEOUT_" + action);
        record.setComment("审批超时（" + node.getTimeoutHours() + "小时），自动处理: " + action);
        record.setApprovalStep(request.getApprovalStep());
        record.setNodeId(request.getCurrentNodeId());
        approvalRecordMapper.insert(record);

        switch (action) {
            case "AUTO_APPROVE" -> {
                skipPendingTasks(request.getId(), request.getCurrentNodeId());
                request.setApprovalStep(request.getApprovalStep() + 1);
                User applicant = userMapper.selectById(request.getApplicantId());
                boolean hasNext = advanceToNextNode(request, applicant);
                if (!hasNext) {
                    request.setStatus("APPROVED");
                    request.setCurrentApproverId(null);
                    request.setCurrentNodeId(null);
                    request.setTimeoutTime(null);
                }
                leaveRequestMapper.updateById(request);
            }
            case "AUTO_REJECT" -> {
                skipPendingTasks(request.getId(), request.getCurrentNodeId());
                request.setStatus("REJECTED");
                request.setCurrentApproverId(null);
                request.setCurrentNodeId(null);
                request.setTimeoutTime(null);
                leaveRequestMapper.updateById(request);
            }
            default -> { // ESCALATE
                if (isParallel(node)) {
                    skipPendingTasks(request.getId(), request.getCurrentNodeId());
                    request.setApprovalStep(request.getApprovalStep() + 1);
                    User applicant = userMapper.selectById(request.getApplicantId());
                    boolean hasNext = advanceToNextNode(request, applicant);
                    if (!hasNext) {
                        request.setStatus("APPROVED");
                        request.setCurrentApproverId(null);
                        request.setCurrentNodeId(null);
                        request.setTimeoutTime(null);
                    }
                } else {
                    Long escalateTo = node.getEscalateToUserId();
                    if (escalateTo != null) {
                        request.setCurrentApproverId(escalateTo);
                        request.setTimeoutTime(LocalDateTime.now().plusHours(node.getTimeoutHours()));
                    } else {
                        request.setApprovalStep(request.getApprovalStep() + 1);
                        User applicant = userMapper.selectById(request.getApplicantId());
                        boolean hasNext = advanceToNextNode(request, applicant);
                        if (!hasNext) {
                            request.setStatus("APPROVED");
                            request.setCurrentApproverId(null);
                            request.setCurrentNodeId(null);
                            request.setTimeoutTime(null);
                        }
                    }
                }
                leaveRequestMapper.updateById(request);
            }
        }
    }

    private void skipPendingTasks(Long requestId, Long nodeId) {
        if (nodeId != null) {
            approvalTaskMapper.update(null,
                    new LambdaUpdateWrapper<ApprovalTask>()
                            .eq(ApprovalTask::getLeaveRequestId, requestId)
                            .eq(ApprovalTask::getNodeId, nodeId)
                            .eq(ApprovalTask::getStatus, "PENDING")
                            .set(ApprovalTask::getStatus, "SKIPPED"));
        }
    }

    // ========== 查询 ==========

    public List<LeaveRequest> getAllRequests() {
        return leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .orderByDesc(LeaveRequest::getCreateTime));
    }

    public List<LeaveRequest> getMyRequests(Long applicantId) {
        return leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getApplicantId, applicantId)
                .orderByDesc(LeaveRequest::getCreateTime));
    }

    public List<LeaveRequest> getPendingRequests(Long approverId) {
        List<LeaveRequest> result = new ArrayList<>(leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getCurrentApproverId, approverId)
                        .eq(LeaveRequest::getStatus, "PENDING")));

        List<ApprovalTask> tasks = approvalTaskMapper.selectList(
                new LambdaQueryWrapper<ApprovalTask>()
                        .eq(ApprovalTask::getApproverId, approverId)
                        .eq(ApprovalTask::getStatus, "PENDING"));

        if (!tasks.isEmpty()) {
            Set<Long> existingIds = result.stream().map(LeaveRequest::getId).collect(Collectors.toSet());
            Set<Long> parallelIds = tasks.stream().map(ApprovalTask::getLeaveRequestId).collect(Collectors.toSet());
            parallelIds.removeAll(existingIds);
            if (!parallelIds.isEmpty()) {
                result.addAll(leaveRequestMapper.selectBatchIds(parallelIds));
            }
        }

        result.sort(Comparator.comparing(LeaveRequest::getCreateTime).reversed());
        return result;
    }

    public List<LeaveRequest> getDoneRequests(Long approverId) {
        List<LeaveRequest> result = new ArrayList<>(leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getCurrentApproverId, approverId)
                        .ne(LeaveRequest::getStatus, "PENDING")));

        List<ApprovalTask> doneTasks = approvalTaskMapper.selectList(
                new LambdaQueryWrapper<ApprovalTask>()
                        .eq(ApprovalTask::getApproverId, approverId)
                        .in(ApprovalTask::getStatus, List.of("COMPLETED", "SKIPPED")));

        if (!doneTasks.isEmpty()) {
            Set<Long> existingIds = result.stream().map(LeaveRequest::getId).collect(Collectors.toSet());
            Set<Long> doneIds = doneTasks.stream().map(ApprovalTask::getLeaveRequestId).collect(Collectors.toSet());
            doneIds.removeAll(existingIds);
            if (!doneIds.isEmpty()) {
                List<LeaveRequest> parallelDone = leaveRequestMapper.selectBatchIds(doneIds);
                parallelDone.removeIf(r -> "PENDING".equals(r.getStatus()));
                result.addAll(parallelDone);
            }
        }

        result.sort(Comparator.comparing(LeaveRequest::getCreateTime).reversed());
        return result;
    }

    public LeaveRequest getRequestDetail(Long requestId) {
        return leaveRequestMapper.selectById(requestId);
    }

    public List<ApprovalRecord> getApprovalRecords(Long requestId) {
        return approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getLeaveRequestId, requestId)
                .orderByAsc(ApprovalRecord::getCreateTime));
    }

    public List<ApprovalTask> getPendingTasks(Long requestId) {
        return approvalTaskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getLeaveRequestId, requestId)
                .eq(ApprovalTask::getStatus, "PENDING"));
    }
}
