package com.smartoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartoa.entity.ApprovalTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTask> {

    /**
     * 将指定申请、指定节点下所有 PENDING 状态的任务标记为 SKIPPED
     *
     * @param requestId 请假申请 ID
     * @param nodeId    审批节点 ID
     * @return 实际更新的行数
     */
    @Update("UPDATE approval_task " +
            "SET status = 'SKIPPED' " +
            "WHERE leave_request_id = #{requestId} " +
            "  AND node_id = #{nodeId} " +
            "  AND status = 'PENDING'")
    int skipPendingByRequestAndNode(@Param("requestId") Long requestId,
                                    @Param("nodeId") Long nodeId);

    /**
     * 统计指定请假申请中某审批人的任务数（含 PENDING/COMPLETED/SKIPPED）
     * 用于判断用户是否为有相关审批任务的审批人
     */
    @Select("SELECT COUNT(*) FROM approval_task " +
            "WHERE leave_request_id = #{requestId} AND approver_id = #{approverId}")
    int countByLeaveRequestIdAndApproverId(@Param("requestId") Long requestId,
                                           @Param("approverId") Long approverId);
}
