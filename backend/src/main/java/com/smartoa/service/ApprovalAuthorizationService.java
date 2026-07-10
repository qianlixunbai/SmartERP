package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 集中对象级访问权限服务。
 * <p>
 * 仅负责判断当前用户是否有权读取指定请假/经费申请，
 * 不处理审批动作、不推进流程。
 * <p>
 * 权限判定优先级：
 * 1. 用户未登录 → 401
 * 2. 申请不存在 → 404
 * 3. 用户是 MANAGER → 允许
 * 4. 用户是申请人 → 允许
 * 5. 用户是当前审批人 → 允许
 * 6. 用户存在该申请的审批记录或审批任务 → 允许
 * 7. 其他情况 → 403
 */
@Service
@RequiredArgsConstructor
public class ApprovalAuthorizationService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final ExpenseRequestMapper expenseRequestMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final ExpenseApprovalTaskMapper expenseApprovalTaskMapper;
    private final AuditLogMapper auditLogMapper;

    /**
     * 校验用户是否有权读取指定请假申请，有权限则返回申请实体。
     *
     * @param requestId 请假申请 ID
     * @param user      当前登录用户（不得为 null）
     * @return 请假申请实体
     * @throws BusinessException 401/404/403
     */
    public LeaveRequest requireReadableLeave(Long requestId, User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "请先登录");
        }

        LeaveRequest request = leaveRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "请假单不存在");
        }

        if ("MANAGER".equals(user.getRole())) {
            return request;
        }

        Long userId = user.getId();

        // 申请人本人
        if (userId.equals(request.getApplicantId())) {
            return request;
        }

        // 当前审批人
        Long currentApproverId = request.getCurrentApproverId();
        if (currentApproverId != null && userId.equals(currentApproverId)) {
            return request;
        }

        // 历史审批人 — 通过审批记录判断
        if (approvalRecordMapper.countByLeaveRequestIdAndApproverId(requestId, userId) > 0) {
            return request;
        }

        // 有相关审批任务（已分配但可能尚未完成）
        if (approvalTaskMapper.countByLeaveRequestIdAndApproverId(requestId, userId) > 0) {
            return request;
        }

        throw new BusinessException(403, "无权限访问该请假单");
    }

    /**
     * 校验用户是否有权读取指定经费申请，有权限则返回申请实体。
     *
     * @param requestId 经费申请 ID
     * @param user      当前登录用户（不得为 null）
     * @return 经费申请实体
     * @throws BusinessException 401/404/403
     */
    public ExpenseRequest requireReadableExpense(Long requestId, User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "请先登录");
        }

        ExpenseRequest request = expenseRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "经费申请不存在");
        }

        if ("MANAGER".equals(user.getRole())) {
            return request;
        }

        Long userId = user.getId();

        // 申请人本人
        if (userId.equals(request.getApplicantId())) {
            return request;
        }

        // 当前审批人
        Long currentApproverId = request.getCurrentApproverId();
        if (currentApproverId != null && userId.equals(currentApproverId)) {
            return request;
        }

        // 历史审批任务参与者
        if (expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(requestId, userId) > 0) {
            return request;
        }

        // 真实审批审计日志中的操作者（仅 APPROVE/REJECT/WITHDRAW）
        if (auditLogMapper.countExpenseApprovalActions(requestId, userId) > 0) {
            return request;
        }

        throw new BusinessException(403, "无权限访问该经费申请");
    }
}
