package com.smartoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartoa.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 统计指定经费申请中某操作者的审批相关审计日志数
     * 仅统计真实审批动作（APPROVE/REJECT/WITHDRAW），排除 SUBMIT/POST/REVERSE
     * 用于判断用户是否为经费申请的历史审批人
     */
    @Select("SELECT COUNT(*) FROM audit_log " +
            "WHERE target_type = 'EXPENSE' " +
            "  AND target_id = #{requestId} " +
            "  AND actor_id = #{actorId} " +
            "  AND action IN ('APPROVE', 'REJECT', 'WITHDRAW')")
    int countExpenseApprovalActions(@Param("requestId") Long requestId,
                                    @Param("actorId") Long actorId);
}
