package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTemplate;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.TemplateField;
import com.smartoa.mapper.ApprovalNodeMapper;
import com.smartoa.mapper.ApprovalRecordMapper;
import com.smartoa.mapper.ApprovalTemplateMapper;
import com.smartoa.mapper.LeaveRequestMapper;
import com.smartoa.mapper.TemplateFieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;

    // ========== 模板 CRUD ==========

    public List<ApprovalTemplate> listAll() {
        return templateMapper.selectList(null);
    }

    public ApprovalTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Transactional
    public void create(ApprovalTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(template);
    }

    @Transactional
    public void update(Long id, ApprovalTemplate data) {
        ApprovalTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        template.setName(data.getName());
        template.setDescription(data.getDescription());
        template.setEnabled(data.isEnabled());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    @Transactional
    public void delete(Long id) {
        templateMapper.deleteById(id);
        // 级联删除节点和字段
        approvalNodeMapper.delete(new LambdaQueryWrapper<ApprovalNode>().eq(ApprovalNode::getTemplateId, id));
        templateFieldMapper.delete(new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getTemplateId, id));
    }

    // ========== 审批节点管理 ==========

    public List<ApprovalNode> listNodes(Long templateId) {
        return approvalNodeMapper.selectList(new LambdaQueryWrapper<ApprovalNode>()
                .eq(ApprovalNode::getTemplateId, templateId)
                .orderByAsc(ApprovalNode::getSortOrder));
    }

    @Transactional
    public void saveNodes(Long templateId, List<ApprovalNode> nodes) {
        List<ApprovalNode> oldNodes = approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getTemplateId, templateId));
        List<Long> oldNodeIds = oldNodes.stream().map(ApprovalNode::getId).collect(Collectors.toList());

        if (!oldNodeIds.isEmpty()) {
            // 清除审批记录中对旧节点的引用
            approvalRecordMapper.update(null,
                    new LambdaUpdateWrapper<ApprovalRecord>()
                            .in(ApprovalRecord::getNodeId, oldNodeIds)
                            .set(ApprovalRecord::getNodeId, null));
            // 清除请假单中对旧节点的引用
            leaveRequestMapper.update(null,
                    new LambdaUpdateWrapper<LeaveRequest>()
                            .in(LeaveRequest::getCurrentNodeId, oldNodeIds)
                            .set(LeaveRequest::getCurrentNodeId, null));
            approvalNodeMapper.deleteByIds(oldNodeIds);
        }

        for (int i = 0; i < nodes.size(); i++) {
            ApprovalNode node = nodes.get(i);
            node.setId(null);
            node.setTemplateId(templateId);
            node.setSortOrder(i);
            node.setCreateTime(LocalDateTime.now());
            node.setUpdateTime(LocalDateTime.now());
            approvalNodeMapper.insert(node);
        }
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        approvalNodeMapper.deleteById(nodeId);
    }

    // ========== 表单字段管理 ==========

    public List<TemplateField> listFields(Long templateId) {
        return templateFieldMapper.selectList(new LambdaQueryWrapper<TemplateField>()
                .eq(TemplateField::getTemplateId, templateId)
                .orderByAsc(TemplateField::getSortOrder));
    }
}
