package com.smartoa.integration;

import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.AuditLog;
import com.smartoa.entity.ExpenseApprovalTask;
import com.smartoa.mapper.ApprovalRecordMapper;
import com.smartoa.mapper.ApprovalTaskMapper;
import com.smartoa.mapper.AuditLogMapper;
import com.smartoa.mapper.ExpenseApprovalTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stabilization Mapper 集成测试（Testcontainers MySQL）")
class StabilizationMapperIntegrationTest extends MySqlIntegrationTestBase {

    @Autowired
    private ApprovalTaskMapper approvalTaskMapper;

    @Autowired
    private ExpenseApprovalTaskMapper expenseApprovalTaskMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM approval_task");
        jdbcTemplate.execute("DELETE FROM approval_record");
        jdbcTemplate.execute("DELETE FROM expense_approval_task");
        jdbcTemplate.execute("DELETE FROM audit_log");
    }

    // ======================== 辅助方法 ========================

    private ApprovalTask leaveTask(Long leaveRequestId, Long nodeId, Long approverId, String status) {
        ApprovalTask t = new ApprovalTask();
        t.setLeaveRequestId(leaveRequestId);
        t.setNodeId(nodeId);
        t.setApproverId(approverId);
        t.setStatus(status);
        return t;
    }

    private ExpenseApprovalTask expenseTask(Long expenseRequestId, Long nodeId, Long approverId, String status) {
        ExpenseApprovalTask t = new ExpenseApprovalTask();
        t.setExpenseRequestId(expenseRequestId);
        t.setNodeId(nodeId);
        t.setApproverId(approverId);
        t.setStatus(status);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        return t;
    }

    private ApprovalRecord leaveRecord(Long leaveRequestId, Long approverId, String action) {
        ApprovalRecord r = new ApprovalRecord();
        r.setLeaveRequestId(leaveRequestId);
        r.setApproverId(approverId);
        r.setAction(action);
        r.setApprovalStep(0);
        return r;
    }

    private AuditLog auditLog(String action, String targetType, Long targetId, Long actorId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActorId(actorId);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    private String taskStatus(String table, String idCol, String reqCol, Long reqId,
                              String approverCol, Long approverId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM " + table + " WHERE " + reqCol + " = ? AND " + approverCol + " = ?",
                String.class, reqId, approverId);
    }

    // ======================== 测试 1：请假驳回任务更新范围 ========================

    @Nested
    @DisplayName("请假驳回任务更新范围")
    class LeaveTaskUpdateScope {

        @Test
        @DisplayName("skipPendingByRequestAndNode 只影响同申请同节点的 PENDING 任务")
        void shouldOnlySkipPendingInSameRequestAndNode() {
            // 插入测试数据
            approvalTaskMapper.insert(leaveTask(10L, 100L, 1L, "PENDING"));
            approvalTaskMapper.insert(leaveTask(10L, 100L, 2L, "PENDING"));
            approvalTaskMapper.insert(leaveTask(10L, 100L, 3L, "COMPLETED"));
            approvalTaskMapper.insert(leaveTask(10L, 200L, 4L, "PENDING"));
            approvalTaskMapper.insert(leaveTask(20L, 100L, 5L, "PENDING"));

            // 将 approver=1 的任务改为 COMPLETED（模拟审批通过）
            ApprovalTask current = approvalTaskMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalTask>()
                            .eq(ApprovalTask::getLeaveRequestId, 10L)
                            .eq(ApprovalTask::getNodeId, 100L)
                            .eq(ApprovalTask::getApproverId, 1L));
            assertNotNull(current);
            current.setStatus("COMPLETED");
            approvalTaskMapper.updateById(current);

            // 执行 skip
            int updated = approvalTaskMapper.skipPendingByRequestAndNode(10L, 100L);

            // 验证更新行数（只有 approver=2 是 PENDING）
            assertEquals(1, updated);

            // 验证最终数据库状态
            assertEquals("COMPLETED", taskStatus("approval_task",
                    "id", "leave_request_id", 10L, "approver_id", 1L));
            assertEquals("SKIPPED", taskStatus("approval_task",
                    "id", "leave_request_id", 10L, "approver_id", 2L));
            assertEquals("COMPLETED", taskStatus("approval_task",
                    "id", "leave_request_id", 10L, "approver_id", 3L));
            assertEquals("PENDING", taskStatus("approval_task",
                    "id", "leave_request_id", 10L, "approver_id", 4L));
            assertEquals("PENDING", taskStatus("approval_task",
                    "id", "leave_request_id", 20L, "approver_id", 5L));
        }
    }

    // ======================== 测试 2：经费驳回任务更新范围 ========================

    @Nested
    @DisplayName("经费驳回任务更新范围")
    class ExpenseTaskUpdateScope {

        @Test
        @DisplayName("skipPendingByRequestAndNode 只影响同申请同节点的 PENDING 任务")
        void shouldOnlySkipPendingInSameRequestAndNode() {
            expenseApprovalTaskMapper.insert(expenseTask(10L, 100L, 1L, "PENDING"));
            expenseApprovalTaskMapper.insert(expenseTask(10L, 100L, 2L, "PENDING"));
            expenseApprovalTaskMapper.insert(expenseTask(10L, 100L, 3L, "COMPLETED"));
            expenseApprovalTaskMapper.insert(expenseTask(10L, 200L, 4L, "PENDING"));
            expenseApprovalTaskMapper.insert(expenseTask(20L, 100L, 5L, "PENDING"));

            // 将 approver=1 改为 COMPLETED
            ExpenseApprovalTask current = expenseApprovalTaskMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExpenseApprovalTask>()
                            .eq(ExpenseApprovalTask::getExpenseRequestId, 10L)
                            .eq(ExpenseApprovalTask::getNodeId, 100L)
                            .eq(ExpenseApprovalTask::getApproverId, 1L));
            assertNotNull(current);
            current.setStatus("COMPLETED");
            expenseApprovalTaskMapper.updateById(current);

            int updated = expenseApprovalTaskMapper.skipPendingByRequestAndNode(10L, 100L);

            assertEquals(1, updated);

            assertEquals("COMPLETED", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 10L, "approver_id", 1L));
            assertEquals("SKIPPED", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 10L, "approver_id", 2L));
            assertEquals("COMPLETED", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 10L, "approver_id", 3L));
            assertEquals("PENDING", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 10L, "approver_id", 4L));
            assertEquals("PENDING", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 20L, "approver_id", 5L));
        }
    }

    // ======================== 测试 3：请假历史授权计数 ========================

    @Nested
    @DisplayName("请假历史授权计数")
    class LeaveAuthorizationCount {

        @Test
        @DisplayName("countByLeaveRequestIdAndApproverId 严格限定 requestId 和 approverId")
        void shouldCountOnlyMatchingRequestAndApprover() {
            // 同 request、目标 approver
            approvalRecordMapper.insert(leaveRecord(100L, 1L, "APPROVE"));
            approvalRecordMapper.insert(leaveRecord(100L, 1L, "REJECT"));
            // 同 request、其他 approver
            approvalRecordMapper.insert(leaveRecord(100L, 2L, "APPROVE"));
            // 其他 request、目标 approver
            approvalRecordMapper.insert(leaveRecord(200L, 1L, "APPROVE"));

            // 应只统计 request=100, approver=1 的 2 条
            assertEquals(2, approvalRecordMapper.countByLeaveRequestIdAndApproverId(100L, 1L));
            // request=100, approver=2 → 1 条
            assertEquals(1, approvalRecordMapper.countByLeaveRequestIdAndApproverId(100L, 2L));
            // request=200, approver=1 → 1 条
            assertEquals(1, approvalRecordMapper.countByLeaveRequestIdAndApproverId(200L, 1L));
            // request=999, approver=1 → 0 条
            assertEquals(0, approvalRecordMapper.countByLeaveRequestIdAndApproverId(999L, 1L));
        }

        @Test
        @DisplayName("approval_task 各状态均可被历史参与计数查到")
        void shouldCountAllTaskStatuses() {
            approvalTaskMapper.insert(leaveTask(100L, 10L, 1L, "PENDING"));
            approvalTaskMapper.insert(leaveTask(100L, 10L, 2L, "COMPLETED"));
            approvalTaskMapper.insert(leaveTask(100L, 10L, 3L, "SKIPPED"));

            // PENDING
            assertEquals(1, approvalTaskMapper.countByLeaveRequestIdAndApproverId(100L, 1L));
            // COMPLETED
            assertEquals(1, approvalTaskMapper.countByLeaveRequestIdAndApproverId(100L, 2L));
            // SKIPPED
            assertEquals(1, approvalTaskMapper.countByLeaveRequestIdAndApproverId(100L, 3L));
            // 不存在
            assertEquals(0, approvalTaskMapper.countByLeaveRequestIdAndApproverId(100L, 99L));
        }
    }

    // ======================== 测试 4：经费任务授权计数 ========================

    @Nested
    @DisplayName("经费任务授权计数")
    class ExpenseTaskAuthorizationCount {

        @Test
        @DisplayName("countByExpenseRequestIdAndApproverId 不会跨申请或跨用户计数")
        void shouldNotCountAcrossRequestOrUser() {
            expenseApprovalTaskMapper.insert(expenseTask(100L, 10L, 1L, "PENDING"));
            expenseApprovalTaskMapper.insert(expenseTask(100L, 10L, 2L, "COMPLETED"));
            expenseApprovalTaskMapper.insert(expenseTask(200L, 10L, 1L, "PENDING"));

            assertEquals(1, expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(100L, 1L));
            assertEquals(1, expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(100L, 2L));
            // 同 approver 但不同 request
            assertEquals(1, expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(200L, 1L));
            // 不存在
            assertEquals(0, expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(100L, 99L));
            assertEquals(0, expenseApprovalTaskMapper.countByExpenseRequestIdAndApproverId(999L, 1L));
        }
    }

    // ======================== 测试 5：经费审计授权动作 ========================

    @Nested
    @DisplayName("经费审计授权动作")
    class ExpenseAuditAuthorizationActions {

        @Test
        @DisplayName("countExpenseApprovalActions 只统计 APPROVE 和 REJECT")
        void shouldOnlyCountApproveAndReject() {
            // 同 request、同 actor 的各种 action
            auditLogMapper.insert(auditLog("APPROVE", "EXPENSE", 100L, 1L));
            auditLogMapper.insert(auditLog("REJECT", "EXPENSE", 100L, 1L));
            auditLogMapper.insert(auditLog("WITHDRAW", "EXPENSE", 100L, 1L));
            auditLogMapper.insert(auditLog("SUBMIT", "EXPENSE", 100L, 1L));
            auditLogMapper.insert(auditLog("POST", "EXPENSE", 100L, 1L));
            auditLogMapper.insert(auditLog("REVERSE", "EXPENSE", 100L, 1L));

            // 只统计 APPROVE + REJECT = 2
            assertEquals(2, auditLogMapper.countExpenseApprovalActions(100L, 1L));

            // 其他 request、同 actor 的 APPROVE → 不计入
            auditLogMapper.insert(auditLog("APPROVE", "EXPENSE", 200L, 1L));
            assertEquals(2, auditLogMapper.countExpenseApprovalActions(100L, 1L));

            // 同 request、其他 actor 的 APPROVE → 不计入
            auditLogMapper.insert(auditLog("APPROVE", "EXPENSE", 100L, 99L));
            assertEquals(2, auditLogMapper.countExpenseApprovalActions(100L, 1L));

            // 验证其他 actor 独立计数
            assertEquals(1, auditLogMapper.countExpenseApprovalActions(100L, 99L));
        }
    }

    // ======================== 测试 6：请假事务回滚 ========================

    @Nested
    @DisplayName("事务回滚")
    class TransactionRollback {

        @Test
        @DisplayName("请假任务状态更新异常时应整体回滚")
        void leaveTaskRollback_ShouldRevertAllChanges() {
            // 插入初始数据
            approvalTaskMapper.insert(leaveTask(50L, 500L, 1L, "PENDING"));
            approvalTaskMapper.insert(leaveTask(50L, 500L, 2L, "PENDING"));

            // 在事务中执行：修改状态 + skip + 然后抛异常
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            assertThrows(RuntimeException.class, () -> txTemplate.execute(status -> {
                ApprovalTask current = approvalTaskMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalTask>()
                                .eq(ApprovalTask::getLeaveRequestId, 50L)
                                .eq(ApprovalTask::getNodeId, 500L)
                                .eq(ApprovalTask::getApproverId, 1L));
                current.setStatus("COMPLETED");
                approvalTaskMapper.updateById(current);

                approvalTaskMapper.skipPendingByRequestAndNode(50L, 500L);

                throw new RuntimeException("模拟异常触发回滚");
            }));

            // 事务结束后重新查询 — 应全部回滚到原始状态
            assertEquals("PENDING", taskStatus("approval_task",
                    "id", "leave_request_id", 50L, "approver_id", 1L));
            assertEquals("PENDING", taskStatus("approval_task",
                    "id", "leave_request_id", 50L, "approver_id", 2L));
        }

        @Test
        @DisplayName("经费任务状态更新异常时应整体回滚")
        void expenseTaskRollback_ShouldRevertAllChanges() {
            expenseApprovalTaskMapper.insert(expenseTask(50L, 500L, 1L, "PENDING"));
            expenseApprovalTaskMapper.insert(expenseTask(50L, 500L, 2L, "PENDING"));

            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            assertThrows(RuntimeException.class, () -> txTemplate.execute(status -> {
                ExpenseApprovalTask current = expenseApprovalTaskMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExpenseApprovalTask>()
                                .eq(ExpenseApprovalTask::getExpenseRequestId, 50L)
                                .eq(ExpenseApprovalTask::getNodeId, 500L)
                                .eq(ExpenseApprovalTask::getApproverId, 1L));
                current.setStatus("COMPLETED");
                expenseApprovalTaskMapper.updateById(current);

                expenseApprovalTaskMapper.skipPendingByRequestAndNode(50L, 500L);

                throw new RuntimeException("模拟异常触发回滚");
            }));

            assertEquals("PENDING", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 50L, "approver_id", 1L));
            assertEquals("PENDING", taskStatus("expense_approval_task",
                    "id", "expense_request_id", 50L, "approver_id", 2L));
        }
    }
}
