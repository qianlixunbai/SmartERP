package com.smartoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartoa.entity.ApprovalTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalTemplateMapper extends BaseMapper<ApprovalTemplate> {

    /**
     * 按主键锁定一行模板（SELECT ... FOR UPDATE），必须在事务中调用。
     * 不查询生成列 active_slot 和 draft_slot。
     */
    @Select("SELECT id, name, description, enabled, create_time, update_time,"
            + " template_key, version_no, workflow_type, lifecycle_status,"
            + " published_at, retired_at, supersedes_id, revision"
            + " FROM approval_template WHERE id = #{id} FOR UPDATE")
    ApprovalTemplate selectByIdForUpdate(@Param("id") Long id);

    /**
     * 按 templateKey 锁定全部版本行（SELECT ... FOR UPDATE），必须在事务中调用。
     * 结果按 version_no ASC, id ASC 排序，不查询生成列。
     */
    @Select("SELECT id, name, description, enabled, create_time, update_time,"
            + " template_key, version_no, workflow_type, lifecycle_status,"
            + " published_at, retired_at, supersedes_id, revision"
            + " FROM approval_template WHERE template_key = #{templateKey}"
            + " ORDER BY version_no ASC, id ASC FOR UPDATE")
    List<ApprovalTemplate> selectVersionsByKeyForUpdate(@Param("templateKey") String templateKey);
}
