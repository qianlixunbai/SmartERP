package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.ApprovalTemplate;
import com.smartoa.entity.AuditLog;
import com.smartoa.entity.ExpenseApprovalTask;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.TemplateField;
import com.smartoa.mapper.ApprovalNodeMapper;
import com.smartoa.mapper.ApprovalRecordMapper;
import com.smartoa.mapper.ApprovalTaskMapper;
import com.smartoa.mapper.ApprovalTemplateMapper;
import com.smartoa.mapper.AuditLogMapper;
import com.smartoa.mapper.ExpenseApprovalTaskMapper;
import com.smartoa.mapper.ExpenseRequestMapper;
import com.smartoa.mapper.LeaveRequestMapper;
import com.smartoa.mapper.TemplateFieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final String DRAFT = "DRAFT";
    private static final String ACTIVE = "ACTIVE";
    private static final String RETIRED = "RETIRED";
    private static final String LEAVE = "LEAVE";
    private static final String EXPENSE = "EXPENSE";
    private static final Pattern TEMPLATE_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{2,63}$");

    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final ExpenseRequestMapper expenseRequestMapper;
    private final ExpenseApprovalTaskMapper expenseApprovalTaskMapper;
    private final AuditLogMapper auditLogMapper;

    // ========== 模板 CRUD ==========

    public List<ApprovalTemplate> listAll() {
        return templateMapper.selectList(null);
    }

    public ApprovalTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Transactional
    public void create(ApprovalTemplate template) {
        validateTemplateMetadata(template);
        validateNewTemplateIdentity(template);
        if (templateMapper.selectCount(new LambdaQueryWrapper<ApprovalTemplate>()
                .eq(ApprovalTemplate::getTemplateKey, template.getTemplateKey())) > 0) {
            throw templateKeyExists();
        }

        LocalDateTime now = LocalDateTime.now();
        template.setId(null);
        template.setVersionNo(1);
        template.setLifecycleStatus(DRAFT);
        template.setEnabled(false);
        template.setPublishedAt(null);
        template.setRetiredAt(null);
        template.setSupersedesId(null);
        template.setRevision(0);
        template.setCreateTime(now);
        template.setUpdateTime(now);
        try {
            templateMapper.insert(template);
        } catch (DataIntegrityViolationException e) {
            throw templateKeyExists();
        }
    }

    @Transactional
    public void update(Long id, ApprovalTemplate data) {
        ApprovalTemplate template = requireMutableDraftForUpdate(id);
        validateTemplateMetadata(data);
        template.setName(data.getName());
        template.setDescription(data.getDescription());
        template.setEnabled(false);
        incrementDraftRevision(template);
        templateMapper.updateById(template);
    }

    @Transactional
    public ApprovalTemplate createDraftFromVersion(Long sourceVersionId) {
        // 1. 锁 source
        ApprovalTemplate source = templateMapper.selectByIdForUpdate(sourceVersionId);
        if (source == null) {
            throw new BusinessException(404, "模板不存在");
        }
        if (source.getLifecycleStatus() == null) {
            throw legacyTemplateImmutable();
        }
        if (!ACTIVE.equals(source.getLifecycleStatus())) {
            throw new BusinessException(409, "只有已发布模板可以复制为草稿");
        }
        if (source.getTemplateKey() == null || source.getVersionNo() == null || source.getWorkflowType() == null) {
            throw new BusinessException(409, "已发布模板版本信息不完整，禁止复制");
        }

        // 2. 锁同 key 所有版本
        List<ApprovalTemplate> allVersions = templateMapper.selectVersionsByKeyForUpdate(source.getTemplateKey());

        // 3. 从锁定结果中重新找到 source 并再次确认
        ApprovalTemplate lockedSource = allVersions.stream()
                .filter(v -> v.getId().equals(sourceVersionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "模板不存在"));
        if (!ACTIVE.equals(lockedSource.getLifecycleStatus())) {
            throw new BusinessException(409, "只有已发布模板可以复制为草稿");
        }

        // 4. 检查是否已有 DRAFT
        if (allVersions.stream().anyMatch(v -> DRAFT.equals(v.getLifecycleStatus()))) {
            throw new BusinessException(409, "该模板已有草稿版本");
        }

        // 5. 在锁定列表中计算最大 versionNo
        int nextVersionNo = allVersions.stream()
                .map(ApprovalTemplate::getVersionNo)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(source.getVersionNo()) + 1;

        LocalDateTime now = LocalDateTime.now();
        ApprovalTemplate draft = new ApprovalTemplate();
        draft.setName(source.getName());
        draft.setDescription(source.getDescription());
        draft.setTemplateKey(source.getTemplateKey());
        draft.setWorkflowType(source.getWorkflowType());
        draft.setVersionNo(nextVersionNo);
        draft.setLifecycleStatus(DRAFT);
        draft.setEnabled(false);
        draft.setPublishedAt(null);
        draft.setRetiredAt(null);
        draft.setSupersedesId(sourceVersionId);
        draft.setRevision(0);
        draft.setCreateTime(now);
        draft.setUpdateTime(now);
        try {
            templateMapper.insert(draft);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(409, "模板版本并发冲突，请重试");
        }

        // 6. 复制节点和字段
        for (ApprovalNode node : listNodes(sourceVersionId)) {
            approvalNodeMapper.insert(copyNode(node, draft.getId(), now));
        }
        for (TemplateField field : listFields(sourceVersionId)) {
            templateFieldMapper.insert(copyField(field, draft.getId(), now));
        }
        return draft;
    }

    @Transactional
    public void delete(Long id) {
        // 1. 先锁模板
        ApprovalTemplate template = requireMutableDraftForUpdate(id);

        // 2. 检查模板引用
        ensureTemplateUnreferenced(template.getId());

        // 3. 查询节点
        List<Long> nodeIds = listNodes(template.getId()).stream().map(ApprovalNode::getId).toList();

        // 4. 检查节点引用
        ensureNodesUnreferenced(nodeIds, "草稿模板已被业务数据引用，禁止删除");

        // 5. 字段→节点→模板顺序删除
        templateFieldMapper.delete(new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getTemplateId, id));
        approvalNodeMapper.delete(new LambdaQueryWrapper<ApprovalNode>().eq(ApprovalNode::getTemplateId, id));
        templateMapper.deleteById(id);
    }

    // ========== 审批节点管理 ==========

    public List<ApprovalNode> listNodes(Long templateId) {
        return approvalNodeMapper.selectList(new LambdaQueryWrapper<ApprovalNode>()
                .eq(ApprovalNode::getTemplateId, templateId)
                .orderByAsc(ApprovalNode::getSortOrder));
    }

    @Transactional
    public void saveNodes(Long templateId, List<ApprovalNode> nodes) {
        // 1. null 参数在数据库访问前返回 400
        if (nodes == null) {
            throw new BusinessException(400, "审批节点不能为空");
        }

        // 2. 先锁模板
        ApprovalTemplate template = requireMutableDraftForUpdate(templateId);

        // 3. 锁定后查询旧节点
        List<ApprovalNode> oldNodes = listNodes(templateId);

        // 4. 六表引用检查
        ensureNodesUnreferenced(oldNodes.stream().map(ApprovalNode::getId).toList());

        // 5. 删除和插入节点
        approvalNodeMapper.delete(new LambdaQueryWrapper<ApprovalNode>()
                .eq(ApprovalNode::getTemplateId, templateId));
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < nodes.size(); i++) {
            ApprovalNode node = nodes.get(i);
            node.setId(null);
            node.setTemplateId(templateId);
            node.setSortOrder(i);
            node.setCreateTime(now);
            node.setUpdateTime(now);
            approvalNodeMapper.insert(node);
        }

        // 6. 递增 revision
        incrementDraftRevision(template);
        templateMapper.updateById(template);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        // 1. 普通读取 node，取得候选 templateId
        ApprovalNode node = approvalNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(404, "审批节点不存在");
        }

        // 2. 锁模板
        ApprovalTemplate template = requireMutableDraftForUpdate(node.getTemplateId());

        // 3. 锁定模板后重新读取 node
        node = approvalNodeMapper.selectById(nodeId);

        // 4. 再次确认 node 仍存在且 templateId 等于已锁模板 ID
        if (node == null) {
            throw new BusinessException(404, "审批节点不存在");
        }
        if (!node.getTemplateId().equals(template.getId())) {
            throw new BusinessException(404, "审批节点不存在");
        }

        // 5. 六表引用检查
        ensureNodesUnreferenced(List.of(nodeId));

        // 6. 删除节点
        approvalNodeMapper.deleteById(nodeId);

        // 7. revision +1
        incrementDraftRevision(template);
        templateMapper.updateById(template);
    }

    // ========== 表单字段管理 ==========

    public List<TemplateField> listFields(Long templateId) {
        return templateFieldMapper.selectList(new LambdaQueryWrapper<TemplateField>()
                .eq(TemplateField::getTemplateId, templateId)
                .orderByAsc(TemplateField::getSortOrder));
    }

    // ========== 私有辅助方法 ==========

    /**
     * 使用 SELECT ... FOR UPDATE 锁定并校验草稿模板。
     * <p>
     * 状态语义：
     * <ul>
     *   <li>不存在 → 404</li>
     *   <li>legacy（lifecycleStatus 为 null）→ 409</li>
     *   <li>ACTIVE → 409</li>
     *   <li>RETIRED → 409</li>
     *   <li>其他非 DRAFT → 409</li>
     * </ul>
     */
    private ApprovalTemplate requireMutableDraftForUpdate(Long templateId) {
        ApprovalTemplate template = templateMapper.selectByIdForUpdate(templateId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        if (template.getLifecycleStatus() == null) {
            throw legacyTemplateImmutable();
        }
        if (ACTIVE.equals(template.getLifecycleStatus())) {
            throw new BusinessException(409, "已发布模板不可修改");
        }
        if (RETIRED.equals(template.getLifecycleStatus())) {
            throw new BusinessException(409, "已退休模板不可修改");
        }
        if (!DRAFT.equals(template.getLifecycleStatus())) {
            throw new BusinessException(409, "非草稿模板不可修改");
        }
        return template;
    }

    private void validateTemplateMetadata(ApprovalTemplate template) {
        if (template == null) {
            throw new BusinessException(400, "模板不能为空");
        }
        if (template.getName() == null || template.getName().isBlank() || template.getName().length() > 100) {
            throw new BusinessException(400, "模板名称不能为空且长度不能超过100");
        }
        if (template.getDescription() != null && template.getDescription().length() > 500) {
            throw new BusinessException(400, "模板描述长度不能超过500");
        }
    }

    private void validateNewTemplateIdentity(ApprovalTemplate template) {
        if (template.getTemplateKey() == null || !TEMPLATE_KEY_PATTERN.matcher(template.getTemplateKey()).matches()) {
            throw new BusinessException(400, "模板标识格式错误");
        }
        if (!LEAVE.equals(template.getWorkflowType()) && !EXPENSE.equals(template.getWorkflowType())) {
            throw new BusinessException(400, "流程类型仅支持 LEAVE 或 EXPENSE");
        }
    }

    private void ensureTemplateUnreferenced(Long templateId) {
        if (leaveRequestMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getTemplateId, templateId)) > 0
                || expenseRequestMapper.selectCount(new LambdaQueryWrapper<ExpenseRequest>()
                .eq(ExpenseRequest::getTemplateId, templateId)) > 0) {
            throw new BusinessException(409, "草稿模板已被业务数据引用，禁止删除");
        }
    }

    private void ensureNodesUnreferenced(List<Long> nodeIds) {
        ensureNodesUnreferenced(nodeIds, "草稿节点已被业务数据引用，禁止替换");
    }

    private void ensureNodesUnreferenced(List<Long> nodeIds, String conflictMessage) {
        if (nodeIds.isEmpty()) {
            return;
        }
        if (approvalRecordMapper.selectCount(new LambdaQueryWrapper<ApprovalRecord>()
                .in(ApprovalRecord::getNodeId, nodeIds)) > 0
                || leaveRequestMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getCurrentNodeId, nodeIds)) > 0
                || approvalTaskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .in(ApprovalTask::getNodeId, nodeIds)) > 0
                || expenseRequestMapper.selectCount(new LambdaQueryWrapper<ExpenseRequest>()
                .in(ExpenseRequest::getCurrentNodeId, nodeIds)) > 0
                || expenseApprovalTaskMapper.selectCount(new LambdaQueryWrapper<ExpenseApprovalTask>()
                .in(ExpenseApprovalTask::getNodeId, nodeIds)) > 0
                || auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getNodeId, nodeIds)) > 0) {
            throw new BusinessException(409, conflictMessage);
        }
    }

    private void incrementDraftRevision(ApprovalTemplate template) {
        template.setRevision((template.getRevision() == null ? 0 : template.getRevision()) + 1);
        template.setUpdateTime(LocalDateTime.now());
        template.setEnabled(false);
    }

    private ApprovalNode copyNode(ApprovalNode source, Long templateId, LocalDateTime now) {
        ApprovalNode node = new ApprovalNode();
        node.setId(null);
        node.setTemplateId(templateId);
        node.setNodeName(source.getNodeName());
        node.setSortOrder(source.getSortOrder());
        node.setApproverType(source.getApproverType());
        node.setApproverId(source.getApproverId());
        node.setConditionExpression(source.getConditionExpression());
        node.setSignType(source.getSignType());
        node.setApproverIds(source.getApproverIds());
        node.setTimeoutHours(source.getTimeoutHours());
        node.setTimeoutAction(source.getTimeoutAction());
        node.setEscalateToUserId(source.getEscalateToUserId());
        node.setCreateTime(now);
        node.setUpdateTime(now);
        return node;
    }

    private TemplateField copyField(TemplateField source, Long templateId, LocalDateTime now) {
        TemplateField field = new TemplateField();
        field.setId(null);
        field.setTemplateId(templateId);
        field.setFieldName(source.getFieldName());
        field.setFieldLabel(source.getFieldLabel());
        field.setFieldType(source.getFieldType());
        field.setRequired(source.getRequired());
        field.setSortOrder(source.getSortOrder());
        field.setOptions(source.getOptions());
        field.setCreateTime(now);
        return field;
    }

    private BusinessException templateKeyExists() {
        return new BusinessException(409, "模板标识已存在");
    }

    private BusinessException legacyTemplateImmutable() {
        return new BusinessException(409, "历史模板尚未迁移，禁止修改");
    }
}