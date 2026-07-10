package com.smartoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartoa.entity.ExpenseApprovalTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExpenseApprovalTaskMapper extends BaseMapper<ExpenseApprovalTask> {

    /**
     * 将指定申请、指定节点下所有 PENDING 状态的任务标记为 SKIPPED
     *
     * @param requestId 经费申请 ID
     * @param nodeId    审批节点 ID
     * @return 实际更新的行数
     */
    @Update("UPDATE expense_approval_task " +
            "SET status = 'SKIPPED' " +
            "WHERE expense_request_id = #{requestId} " +
            "  AND node_id = #{nodeId} " +
            "  AND status = 'PENDING'")
    int skipPendingByRequestAndNode(@Param("requestId") Long requestId,
                                    @Param("nodeId") Long nodeId);
}
