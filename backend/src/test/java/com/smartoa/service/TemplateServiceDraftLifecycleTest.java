package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ApprovalTemplate;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceDraftLifecycleTest {

    private ApprovalTemplateMapper templateMapper;
    private ApprovalNodeMapper nodeMapper;
    private TemplateFieldMapper fieldMapper;
    private ApprovalRecordMapper recordMapper;
    private LeaveRequestMapper leaveMapper;
    private ApprovalTaskMapper taskMapper;
    private ExpenseRequestMapper expenseMapper;
    private ExpenseApprovalTaskMapper expenseTaskMapper;
    private AuditLogMapper auditLogMapper;
    private TemplateService service;

    @BeforeEach
    void setUp() {
        templateMapper = mock(ApprovalTemplateMapper.class);
        nodeMapper = mock(ApprovalNodeMapper.class);
        fieldMapper = mock(TemplateFieldMapper.class);
        recordMapper = mock(ApprovalRecordMapper.class);
        leaveMapper = mock(LeaveRequestMapper.class);
        taskMapper = mock(ApprovalTaskMapper.class);
        expenseMapper = mock(ExpenseRequestMapper.class);
        expenseTaskMapper = mock(ExpenseApprovalTaskMapper.class);
        auditLogMapper = mock(AuditLogMapper.class);
        service = new TemplateService(templateMapper, nodeMapper, fieldMapper, recordMapper, leaveMapper,
                taskMapper, expenseMapper, expenseTaskMapper, auditLogMapper);
    }

    @Test
    void createForcesNewTemplateToDraftVersionOneAndIgnoresClientLifecycleFields() {
        ApprovalTemplate input = validTemplate();
        input.setId(88L);
        input.setVersionNo(7);
        input.setLifecycleStatus("ACTIVE");
        input.setEnabled(true);
        input.setPublishedAt(LocalDateTime.now().minusDays(1));
        input.setRetiredAt(LocalDateTime.now());
        input.setSupersedesId(9L);
        input.setRevision(11);
        input.setCreateTime(LocalDateTime.MIN);
        input.setUpdateTime(LocalDateTime.MIN);
        when(templateMapper.selectCount(any())).thenReturn(0L);

        service.create(input);

        assertAll(
                () -> assertNull(input.getId()),
                () -> assertEquals(1, input.getVersionNo()),
                () -> assertEquals("DRAFT", input.getLifecycleStatus()),
                () -> assertFalse(input.isEnabled()),
                () -> assertNull(input.getPublishedAt()),
                () -> assertNull(input.getRetiredAt()),
                () -> assertNull(input.getSupersedesId()),
                () -> assertEquals(0, input.getRevision()),
                () -> assertNotNull(input.getCreateTime()),
                () -> assertNotNull(input.getUpdateTime()));
        ArgumentCaptor<ApprovalTemplate> captor = ArgumentCaptor.forClass(ApprovalTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertSame(input, captor.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"leave", "A", "AB", "ABC-DEF", "ABC def"})
    void createRejectsInvalidTemplateKey(String key) {
        ApprovalTemplate input = validTemplate();
        input.setTemplateKey(key);

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(input));

        assertEquals(400, error.getCode());
        verifyNoInteractions(templateMapper);
    }

    @Test
    void createRejectsUnsupportedWorkflowType() {
        ApprovalTemplate input = validTemplate();
        input.setWorkflowType("OTHER");

        assertEquals(400, assertThrows(BusinessException.class, () -> service.create(input)).getCode());
        verifyNoInteractions(templateMapper);
    }

    @Test
    void createRejectsEmptyOrOverlongNameAndOverlongDescription() {
        ApprovalTemplate emptyName = validTemplate();
        emptyName.setName("");
        ApprovalTemplate longName = validTemplate();
        longName.setName("n".repeat(101));
        ApprovalTemplate longDescription = validTemplate();
        longDescription.setDescription("d".repeat(501));

        assertAll(
                () -> assertEquals(400, assertThrows(BusinessException.class, () -> service.create(emptyName)).getCode()),
                () -> assertEquals(400, assertThrows(BusinessException.class, () -> service.create(longName)).getCode()),
                () -> assertEquals(400, assertThrows(BusinessException.class, () -> service.create(longDescription)).getCode()));
        verifyNoInteractions(templateMapper);
    }

    @Test
    void createRejectsNullName() {
        ApprovalTemplate input = validTemplate();
        input.setName(null);

        assertEquals(400, assertThrows(BusinessException.class, () -> service.create(input)).getCode());
        verifyNoInteractions(templateMapper);
    }

    @Test
    void createRejectsWhitespaceOnlyNameBeforeMapperAccess() {
        ApprovalTemplate input = validTemplate();
        input.setName("   ");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(input));

        assertEquals(400, error.getCode());
        assertEquals("模板名称不能为空且长度不能超过100", error.getMessage());
        verifyNoInteractions(templateMapper);
    }

    @Test
    void createPreservesNonBlankNameWithoutTrimming() {
        ApprovalTemplate input = validTemplate();
        input.setName(" Leave template ");
        when(templateMapper.selectCount(any())).thenReturn(0L);

        service.create(input);

        assertEquals(" Leave template ", input.getName());
        verify(templateMapper).insert(input);
    }

    @Test
    void createRejectsExistingTemplateKeyBeforeInsert() {
        when(templateMapper.selectCount(any())).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(validTemplate()));

        assertEquals(409, error.getCode());
        assertEquals("模板标识已存在", error.getMessage());
        verify(templateMapper, never()).insert(any(ApprovalTemplate.class));
    }

    @Test
    void createPreservesValidTemplateKeyWithoutNormalization() {
        ApprovalTemplate input = validTemplate();
        input.setTemplateKey("LEAVE_FLOW_01");
        when(templateMapper.selectCount(any())).thenReturn(0L);

        service.create(input);

        assertEquals("LEAVE_FLOW_01", input.getTemplateKey());
        verify(templateMapper).insert(input);
    }

    @Test
    void createMapsDatabaseUniqueConflictToTemplateKeyConflict() {
        when(templateMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("duplicate")).when(templateMapper).insert(any(ApprovalTemplate.class));

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(validTemplate()));

        assertEquals(409, error.getCode());
        assertEquals("模板标识已存在", error.getMessage());
    }

    @Test
    void updateDraftChangesOnlyMetadataAndIncrementsRevision() {
        ApprovalTemplate stored = draft(12L);
        stored.setRevision(4);
        stored.setEnabled(false);
        stored.setTemplateKey("LEAVE_FLOW");
        stored.setVersionNo(2);
        stored.setWorkflowType("LEAVE");
        ApprovalTemplate data = validTemplate();
        data.setName("new name");
        data.setDescription("new description");
        data.setTemplateKey("EXPENSE_FLOW");
        data.setVersionNo(99);
        data.setLifecycleStatus("ACTIVE");
        data.setWorkflowType("EXPENSE");
        data.setEnabled(true);
        when(templateMapper.selectById(12L)).thenReturn(stored);

        service.update(12L, data);

        assertAll(
                () -> assertEquals("new name", stored.getName()),
                () -> assertEquals("new description", stored.getDescription()),
                () -> assertEquals("LEAVE_FLOW", stored.getTemplateKey()),
                () -> assertEquals(2, stored.getVersionNo()),
                () -> assertEquals("LEAVE", stored.getWorkflowType()),
                () -> assertEquals("DRAFT", stored.getLifecycleStatus()),
                () -> assertFalse(stored.isEnabled()),
                () -> assertEquals(5, stored.getRevision()),
                () -> assertNotNull(stored.getUpdateTime()));
        verify(templateMapper).updateById(stored);
    }

    @Test
    void updateTreatsNullRevisionAsZero() {
        ApprovalTemplate stored = draft(12L);
        stored.setRevision(null);
        when(templateMapper.selectById(12L)).thenReturn(stored);

        service.update(12L, validTemplate());

        assertEquals(1, stored.getRevision());
    }

    @Test
    void updateRejectsNullDataWithoutMutatingDraft() {
        ApprovalTemplate stored = draft(12L);
        stored.setName("stored name");
        stored.setDescription("stored description");
        stored.setRevision(4);
        stored.setUpdateTime(LocalDateTime.MIN);
        stored.setEnabled(false);
        when(templateMapper.selectById(12L)).thenReturn(stored);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, null));

        assertAll(
                () -> assertEquals(400, error.getCode()),
                () -> assertEquals("模板不能为空", error.getMessage()),
                () -> assertEquals("stored name", stored.getName()),
                () -> assertEquals("stored description", stored.getDescription()),
                () -> assertEquals(4, stored.getRevision()),
                () -> assertEquals(LocalDateTime.MIN, stored.getUpdateTime()),
                () -> assertFalse(stored.isEnabled()));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void updateRejectsNullEmptyAndWhitespaceOnlyNameWithoutMutatingDraft(String invalidName) {
        ApprovalTemplate stored = draft(12L);
        stored.setName("stored name");
        stored.setDescription("stored description");
        stored.setRevision(4);
        stored.setUpdateTime(LocalDateTime.MIN);
        stored.setEnabled(false);
        ApprovalTemplate data = validTemplate();
        data.setName(invalidName);
        data.setDescription("new description");
        when(templateMapper.selectById(12L)).thenReturn(stored);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, data));

        assertAll(
                () -> assertEquals(400, error.getCode()),
                () -> assertEquals("模板名称不能为空且长度不能超过100", error.getMessage()),
                () -> assertEquals("stored name", stored.getName()),
                () -> assertEquals("stored description", stored.getDescription()),
                () -> assertEquals(4, stored.getRevision()),
                () -> assertEquals(LocalDateTime.MIN, stored.getUpdateTime()),
                () -> assertFalse(stored.isEnabled()));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @Test
    void updateRejectsOverlongNameWithoutMutatingDraft() {
        ApprovalTemplate stored = draft(12L);
        stored.setName("stored name");
        stored.setDescription("stored description");
        stored.setRevision(4);
        stored.setUpdateTime(LocalDateTime.MIN);
        ApprovalTemplate data = validTemplate();
        data.setName("n".repeat(101));
        when(templateMapper.selectById(12L)).thenReturn(stored);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, data));

        assertAll(
                () -> assertEquals(400, error.getCode()),
                () -> assertEquals("模板名称不能为空且长度不能超过100", error.getMessage()),
                () -> assertEquals("stored name", stored.getName()),
                () -> assertEquals("stored description", stored.getDescription()),
                () -> assertEquals(4, stored.getRevision()),
                () -> assertEquals(LocalDateTime.MIN, stored.getUpdateTime()));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @Test
    void updateRejectsOverlongDescriptionWithoutMutatingDraft() {
        ApprovalTemplate stored = draft(12L);
        stored.setName("stored name");
        stored.setDescription("stored description");
        stored.setRevision(4);
        stored.setUpdateTime(LocalDateTime.MIN);
        ApprovalTemplate data = validTemplate();
        data.setName("new name");
        data.setDescription("d".repeat(501));
        when(templateMapper.selectById(12L)).thenReturn(stored);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, data));

        assertAll(
                () -> assertEquals(400, error.getCode()),
                () -> assertEquals("模板描述长度不能超过500", error.getMessage()),
                () -> assertEquals("stored name", stored.getName()),
                () -> assertEquals("stored description", stored.getDescription()),
                () -> assertEquals(4, stored.getRevision()),
                () -> assertEquals(LocalDateTime.MIN, stored.getUpdateTime()));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @Test
    void updateAcceptsMetadataAtLengthBoundaries() {
        ApprovalTemplate stored = draft(12L);
        stored.setRevision(4);
        ApprovalTemplate data = validTemplate();
        data.setName("n".repeat(100));
        data.setDescription("d".repeat(500));
        when(templateMapper.selectById(12L)).thenReturn(stored);

        service.update(12L, data);

        assertAll(
                () -> assertEquals("n".repeat(100), stored.getName()),
                () -> assertEquals("d".repeat(500), stored.getDescription()),
                () -> assertEquals(5, stored.getRevision()),
                () -> assertNotNull(stored.getUpdateTime()),
                () -> assertFalse(stored.isEnabled()));
        verify(templateMapper).updateById(stored);
    }

    @Test
    void updatePrioritizesMissingAndImmutableTemplateErrorsOverNullData() {
        ApprovalTemplate active = active(12L);
        when(templateMapper.selectById(12L)).thenReturn(active);
        when(templateMapper.selectById(13L)).thenReturn(null);

        assertAll(
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.update(12L, null)).getCode()),
                () -> assertEquals(404, assertThrows(BusinessException.class, () -> service.update(13L, null)).getCode()));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @Test
    void updateRejectsMissingTemplate() {
        when(templateMapper.selectById(12L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, validTemplate()));

        assertEquals(404, error.getCode());
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "RETIRED"})
    void updateRejectsNonDraftLifecycle(String status) {
        ApprovalTemplate stored = draft(12L);
        stored.setLifecycleStatus(status);
        when(templateMapper.selectById(12L)).thenReturn(stored);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, validTemplate()));

        assertEquals(409, error.getCode());
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @Test
    void updateRejectsLegacyTemplate() {
        ApprovalTemplate legacy = draft(12L);
        legacy.setLifecycleStatus(null);
        when(templateMapper.selectById(12L)).thenReturn(legacy);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(12L, validTemplate()));

        assertEquals(409, error.getCode());
        assertEquals("历史模板尚未迁移，禁止修改", error.getMessage());
    }

    @Test
    void createDraftFromActiveVersionCopiesNodesAndFieldsWithoutChangingSource() {
        ApprovalTemplate source = active(7L);
        source.setVersionNo(3);
        source.setDescription("source description");
        ApprovalNode sourceNode = node(71L, 7L);
        sourceNode.setApproverIds("2,3");
        sourceNode.setTimeoutHours(24);
        sourceNode.setTimeoutAction("ESCALATE");
        sourceNode.setEscalateToUserId(8L);
        TemplateField sourceField = field(81L, 7L);
        when(templateMapper.selectById(7L)).thenReturn(source);
        when(templateMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.selectList(any())).thenReturn(List.of(source));
        when(nodeMapper.selectList(any())).thenReturn(List.of(sourceNode));
        when(fieldMapper.selectList(any())).thenReturn(List.of(sourceField));
        doAnswer(invocation -> {
            ApprovalTemplate draft = invocation.getArgument(0);
            draft.setId(9L);
            return 1;
        }).when(templateMapper).insert(any(ApprovalTemplate.class));

        ApprovalTemplate draft = service.createDraftFromVersion(7L);

        assertAll(
                () -> assertEquals(9L, draft.getId()),
                () -> assertEquals(4, draft.getVersionNo()),
                () -> assertEquals("DRAFT", draft.getLifecycleStatus()),
                () -> assertFalse(draft.isEnabled()),
                () -> assertEquals(7L, draft.getSupersedesId()),
                () -> assertEquals(0, draft.getRevision()),
                () -> assertEquals("ACTIVE", source.getLifecycleStatus()),
                () -> assertEquals(3, source.getVersionNo()));
        ArgumentCaptor<ApprovalNode> nodeCaptor = ArgumentCaptor.forClass(ApprovalNode.class);
        verify(nodeMapper).insert(nodeCaptor.capture());
        ApprovalNode copiedNode = nodeCaptor.getValue();
        assertAll(
                () -> assertNull(copiedNode.getId()),
                () -> assertEquals(9L, copiedNode.getTemplateId()),
                () -> assertEquals(sourceNode.getNodeName(), copiedNode.getNodeName()),
                () -> assertEquals(sourceNode.getApproverIds(), copiedNode.getApproverIds()),
                () -> assertEquals(sourceNode.getTimeoutHours(), copiedNode.getTimeoutHours()),
                () -> assertEquals(sourceNode.getTimeoutAction(), copiedNode.getTimeoutAction()),
                () -> assertEquals(sourceNode.getEscalateToUserId(), copiedNode.getEscalateToUserId()));
        ArgumentCaptor<TemplateField> fieldCaptor = ArgumentCaptor.forClass(TemplateField.class);
        verify(fieldMapper).insert(fieldCaptor.capture());
        assertAll(
                () -> assertNull(fieldCaptor.getValue().getId()),
                () -> assertEquals(9L, fieldCaptor.getValue().getTemplateId()),
                () -> assertEquals(sourceField.getFieldName(), fieldCaptor.getValue().getFieldName()),
                () -> assertEquals(sourceField.getFieldLabel(), fieldCaptor.getValue().getFieldLabel()),
                () -> assertEquals(sourceField.getFieldType(), fieldCaptor.getValue().getFieldType()),
                () -> assertEquals(sourceField.getRequired(), fieldCaptor.getValue().getRequired()),
                () -> assertEquals(sourceField.getSortOrder(), fieldCaptor.getValue().getSortOrder()),
                () -> assertEquals(sourceField.getOptions(), fieldCaptor.getValue().getOptions()));
    }

    @Test
    void createDraftRejectsMissingAndIncompleteSourceVersions() {
        when(templateMapper.selectById(7L)).thenReturn(null);
        ApprovalTemplate incomplete = active(8L);
        incomplete.setWorkflowType(null);
        when(templateMapper.selectById(8L)).thenReturn(incomplete);

        assertAll(
                () -> assertEquals(404, assertThrows(BusinessException.class, () -> service.createDraftFromVersion(7L)).getCode()),
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.createDraftFromVersion(8L)).getCode()));
        verify(templateMapper, never()).insert(any(ApprovalTemplate.class));
    }

    @Test
    void createDraftUsesHighestExistingVersionNumberAndMapsUniqueConflict() {
        ApprovalTemplate source = active(7L);
        source.setVersionNo(3);
        ApprovalTemplate latest = active(8L);
        latest.setVersionNo(7);
        when(templateMapper.selectById(7L)).thenReturn(source);
        when(templateMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.selectList(any())).thenReturn(List.of(source, latest));
        doThrow(new DuplicateKeyException("duplicate")).when(templateMapper).insert(any(ApprovalTemplate.class));

        BusinessException error = assertThrows(BusinessException.class, () -> service.createDraftFromVersion(7L));

        assertEquals(409, error.getCode());
        ArgumentCaptor<ApprovalTemplate> captor = ArgumentCaptor.forClass(ApprovalTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertEquals(8, captor.getValue().getVersionNo());
        verifyNoInteractions(nodeMapper, fieldMapper);
    }

    @Test
    void createDraftRejectsWhenDraftAlreadyExistsWithoutInsert() {
        when(templateMapper.selectById(7L)).thenReturn(active(7L));
        when(templateMapper.selectCount(any())).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.createDraftFromVersion(7L));

        assertEquals(409, error.getCode());
        verify(templateMapper, never()).insert(any(ApprovalTemplate.class));
        verifyNoInteractions(nodeMapper, fieldMapper);
    }

    @Test
    void createDraftRejectsRetiredAndLegacySources() {
        ApprovalTemplate retired = active(7L);
        retired.setLifecycleStatus("RETIRED");
        ApprovalTemplate legacy = active(8L);
        legacy.setLifecycleStatus(null);
        when(templateMapper.selectById(7L)).thenReturn(retired);
        when(templateMapper.selectById(8L)).thenReturn(legacy);

        assertAll(
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.createDraftFromVersion(7L)).getCode()),
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.createDraftFromVersion(8L)).getCode()));
        verify(templateMapper, never()).insert(any(ApprovalTemplate.class));
    }

    @Test
    void createDraftPropagatesNodeCopyFailure() {
        when(templateMapper.selectById(7L)).thenReturn(active(7L));
        when(templateMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.selectList(any())).thenReturn(List.of(active(7L)));
        when(nodeMapper.selectList(any())).thenReturn(List.of(node(71L, 7L)));
        doAnswer(invocation -> {
            invocation.<ApprovalTemplate>getArgument(0).setId(9L);
            return 1;
        }).when(templateMapper).insert(any(ApprovalTemplate.class));
        doThrow(new IllegalStateException("node copy failed")).when(nodeMapper).insert(any(ApprovalNode.class));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.createDraftFromVersion(7L));

        assertEquals("node copy failed", error.getMessage());
        verifyNoInteractions(fieldMapper);
    }

    @Test
    void saveNodesRewritesClientControlledFieldsAndIncrementsDraftRevision() {
        ApprovalTemplate template = draft(5L);
        template.setRevision(2);
        ApprovalNode incoming = node(99L, 100L);
        incoming.setSortOrder(44);
        incoming.setCreateTime(LocalDateTime.MIN);
        incoming.setUpdateTime(LocalDateTime.MIN);
        when(templateMapper.selectById(5L)).thenReturn(template);
        when(nodeMapper.selectList(any())).thenReturn(List.of());

        service.saveNodes(5L, List.of(incoming));

        assertAll(
                () -> assertNull(incoming.getId()),
                () -> assertEquals(5L, incoming.getTemplateId()),
                () -> assertEquals(0, incoming.getSortOrder()),
                () -> assertNotEquals(LocalDateTime.MIN, incoming.getCreateTime()),
                () -> assertNotEquals(LocalDateTime.MIN, incoming.getUpdateTime()),
                () -> assertEquals(3, template.getRevision()));
        InOrder order = inOrder(nodeMapper, templateMapper);
        order.verify(nodeMapper).delete(any());
        order.verify(nodeMapper).insert(incoming);
        order.verify(templateMapper).updateById(template);
    }

    @Test
    void saveNodesRejectsNullListBeforeAnyDataAccess() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.saveNodes(5L, null));

        assertEquals(400, error.getCode());
        verifyNoInteractions(templateMapper, nodeMapper, fieldMapper, recordMapper, leaveMapper, taskMapper,
                expenseMapper, expenseTaskMapper, auditLogMapper);
    }

    @Test
    void saveNodesAllowsEmptyListForUnreferencedDraft() {
        ApprovalTemplate template = draft(5L);
        when(templateMapper.selectById(5L)).thenReturn(template);
        when(nodeMapper.selectList(any())).thenReturn(List.of());

        service.saveNodes(5L, List.of());

        verify(nodeMapper).delete(any());
        verify(nodeMapper, never()).insert(any(ApprovalNode.class));
        verify(templateMapper).updateById(template);
    }

    @Test
    void saveNodesRejectsActiveRetiredAndLegacyTemplatesBeforeNodeMutation() {
        ApprovalTemplate active = draft(5L);
        active.setLifecycleStatus("ACTIVE");
        ApprovalTemplate retired = draft(6L);
        retired.setLifecycleStatus("RETIRED");
        ApprovalTemplate legacy = draft(7L);
        legacy.setLifecycleStatus(null);
        when(templateMapper.selectById(5L)).thenReturn(active);
        when(templateMapper.selectById(6L)).thenReturn(retired);
        when(templateMapper.selectById(7L)).thenReturn(legacy);

        assertAll(
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.saveNodes(5L, List.of())).getCode()),
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.saveNodes(6L, List.of())).getCode()),
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.saveNodes(7L, List.of())).getCode()));
        verify(nodeMapper, never()).delete(any());
        verify(nodeMapper, never()).insert(any(ApprovalNode.class));
    }

    @Test
    void saveNodesStopsWithZeroWritesWhenHistoricNodeReferenceExists() {
        ApprovalTemplate template = draft(5L);
        when(templateMapper.selectById(5L)).thenReturn(template);
        when(nodeMapper.selectList(any())).thenReturn(List.of(node(51L, 5L)));
        when(recordMapper.selectCount(any())).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveNodes(5L, List.of(node(99L, 5L))));

        assertEquals(409, error.getCode());
        verify(nodeMapper, never()).delete(any());
        verify(nodeMapper, never()).insert(any(ApprovalNode.class));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
        verify(recordMapper).selectCount(any());
        verifyNoInteractions(leaveMapper, taskMapper, expenseMapper, expenseTaskMapper, auditLogMapper);
    }

    @Test
    void saveNodesStopsWithZeroWritesWhenExpenseCurrentNodeReferencesOldNode() {
        ApprovalTemplate template = draft(5L);
        when(templateMapper.selectById(5L)).thenReturn(template);
        when(nodeMapper.selectList(any())).thenReturn(List.of(node(51L, 5L)));
        when(expenseMapper.selectCount(any())).thenReturn(1L);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.saveNodes(5L, List.of(node(99L, 5L)))).getCode());

        verify(nodeMapper, never()).delete(any());
        verify(nodeMapper, never()).insert(any(ApprovalNode.class));
        verify(templateMapper, never()).updateById(any(ApprovalTemplate.class));
    }

    @Test
    void deleteNodeOnlyDeletesUnreferencedDraftNodeAndBumpsRevision() {
        ApprovalTemplate template = draft(5L);
        template.setRevision(null);
        when(nodeMapper.selectById(51L)).thenReturn(node(51L, 5L));
        when(templateMapper.selectById(5L)).thenReturn(template);

        service.deleteNode(51L);

        InOrder order = inOrder(nodeMapper, templateMapper);
        order.verify(nodeMapper).deleteById((java.io.Serializable) 51L);
        order.verify(templateMapper).updateById(template);
        assertEquals(1, template.getRevision());
    }

    @Test
    void deleteNodeRejectsMissingNodeAndReferencedNodeWithoutDelete() {
        when(nodeMapper.selectById(51L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class, () -> service.deleteNode(51L)).getCode());
        verify(nodeMapper, never()).deleteById(any(java.io.Serializable.class));

        reset(nodeMapper);
        when(nodeMapper.selectById(51L)).thenReturn(node(51L, 5L));
        when(templateMapper.selectById(5L)).thenReturn(draft(5L));
        when(taskMapper.selectCount(any())).thenReturn(1L);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.deleteNode(51L)).getCode());
        verify(nodeMapper, never()).deleteById((java.io.Serializable) 51L);
    }

    @Test
    void deleteNodeRejectsActiveTemplateBeforeReferenceChecks() {
        ApprovalTemplate active = active(5L);
        when(nodeMapper.selectById(51L)).thenReturn(node(51L, 5L));
        when(templateMapper.selectById(5L)).thenReturn(active);

        assertEquals(409, assertThrows(BusinessException.class, () -> service.deleteNode(51L)).getCode());
        verify(nodeMapper, never()).deleteById((java.io.Serializable) 51L);
        verifyNoInteractions(recordMapper, leaveMapper, taskMapper, expenseMapper, expenseTaskMapper, auditLogMapper);
    }

    @Test
    void deleteDraftDeletesFieldsThenNodesThenTemplate() {
        ApprovalTemplate template = draft(5L);
        when(templateMapper.selectById(5L)).thenReturn(template);
        when(nodeMapper.selectList(any())).thenReturn(List.of());

        service.delete(5L);

        InOrder order = inOrder(fieldMapper, nodeMapper, templateMapper);
        order.verify(fieldMapper).delete(any());
        order.verify(nodeMapper).delete(any());
        order.verify(templateMapper).deleteById((java.io.Serializable) 5L);
    }

    @Test
    void deleteDraftRejectsTemplateReferencesBeforeAnyDelete() {
        when(templateMapper.selectById(5L)).thenReturn(draft(5L));
        when(leaveMapper.selectCount(any())).thenReturn(1L);

        assertEquals(409, assertThrows(BusinessException.class, () -> service.delete(5L)).getCode());

        verify(fieldMapper, never()).delete(any());
        verify(nodeMapper, never()).delete(any());
        verify(templateMapper, never()).deleteById(any(java.io.Serializable.class));
        verifyNoInteractions(expenseMapper, recordMapper, taskMapper, expenseTaskMapper, auditLogMapper);
    }

    @Test
    void deleteDraftRejectsExpenseAndNodeReferencesBeforeAnyDelete() {
        when(templateMapper.selectById(5L)).thenReturn(draft(5L));
        when(expenseMapper.selectCount(any())).thenReturn(1L);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.delete(5L)).getCode());
        verify(fieldMapper, never()).delete(any());
        verify(nodeMapper, never()).delete(any());
        verify(templateMapper, never()).deleteById(any(java.io.Serializable.class));

        reset(expenseMapper);
        when(templateMapper.selectById(6L)).thenReturn(draft(6L));
        when(nodeMapper.selectList(any())).thenReturn(List.of(node(61L, 6L)));
        when(auditLogMapper.selectCount(any())).thenReturn(1L);
        BusinessException nodeReferenceError = assertThrows(BusinessException.class, () -> service.delete(6L));
        assertEquals(409, nodeReferenceError.getCode());
        assertEquals("草稿模板已被业务数据引用，禁止删除", nodeReferenceError.getMessage());
        verify(fieldMapper, never()).delete(any());
        verify(nodeMapper, never()).delete(any());
        verify(templateMapper, never()).deleteById(6L);
    }

    @Test
    void deleteRejectsActiveRetiredAndLegacyTemplates() {
        ApprovalTemplate active = draft(5L);
        active.setLifecycleStatus("ACTIVE");
        ApprovalTemplate retired = draft(6L);
        retired.setLifecycleStatus("RETIRED");
        ApprovalTemplate legacy = draft(7L);
        legacy.setLifecycleStatus(null);
        when(templateMapper.selectById(5L)).thenReturn(active);
        when(templateMapper.selectById(6L)).thenReturn(retired);
        when(templateMapper.selectById(7L)).thenReturn(legacy);

        assertAll(
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.delete(5L)).getCode()),
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.delete(6L)).getCode()),
                () -> assertEquals(409, assertThrows(BusinessException.class, () -> service.delete(7L)).getCode()));
        verify(fieldMapper, never()).delete(any());
        verify(nodeMapper, never()).delete(any());
        verify(templateMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void deleteRejectsMissingTemplate() {
        when(templateMapper.selectById(5L)).thenReturn(null);

        assertEquals(404, assertThrows(BusinessException.class, () -> service.delete(5L)).getCode());
        verifyNoInteractions(fieldMapper, nodeMapper, recordMapper, leaveMapper, taskMapper, expenseMapper,
                expenseTaskMapper, auditLogMapper);
        verify(templateMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    private ApprovalTemplate validTemplate() {
        ApprovalTemplate template = new ApprovalTemplate();
        template.setName("Leave template");
        template.setDescription("description");
        template.setTemplateKey("LEAVE_FLOW");
        template.setWorkflowType("LEAVE");
        return template;
    }

    private ApprovalTemplate draft(Long id) {
        ApprovalTemplate template = validTemplate();
        template.setId(id);
        template.setVersionNo(1);
        template.setLifecycleStatus("DRAFT");
        template.setEnabled(false);
        template.setRevision(0);
        return template;
    }

    private ApprovalTemplate active(Long id) {
        ApprovalTemplate template = draft(id);
        template.setLifecycleStatus("ACTIVE");
        return template;
    }

    private ApprovalNode node(Long id, Long templateId) {
        ApprovalNode node = new ApprovalNode();
        node.setId(id);
        node.setTemplateId(templateId);
        node.setNodeName("Manager");
        node.setSortOrder(1);
        node.setApproverType("USER");
        node.setApproverId(2L);
        node.setConditionExpression("amount > 0");
        node.setSignType("OR");
        return node;
    }

    private TemplateField field(Long id, Long templateId) {
        TemplateField field = new TemplateField();
        field.setId(id);
        field.setTemplateId(templateId);
        field.setFieldName("reason");
        field.setFieldLabel("Reason");
        field.setFieldType("TEXT");
        field.setRequired(true);
        field.setSortOrder(2);
        field.setOptions("[]");
        return field;
    }
}
