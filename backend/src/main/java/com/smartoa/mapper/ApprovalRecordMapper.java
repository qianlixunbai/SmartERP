package com.smartoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartoa.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    /**
     * 统计指定请假申请中某审批人的审批记录数
     * 用于判断用户是否为历史审批人
     */
    @Select("SELECT COUNT(*) FROM approval_record " +
            "WHERE leave_request_id = #{requestId} AND approver_id = #{approverId}")
    int countByLeaveRequestIdAndApproverId(@Param("requestId") Long requestId,
                                           @Param("approverId") Long approverId);
}
