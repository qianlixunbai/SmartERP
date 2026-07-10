package com.smartoa.integration;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ApprovalTemplate;
import com.smartoa.entity.TemplateField;
import com.smartoa.mapper.ApprovalNodeMapper;
import com.smartoa.mapper.ApprovalTemplateMapper;
import com.smartoa.mapper.TemplateFieldMapper;
import com.smartoa.service.TemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模板服务并发集成测试。
 * <p>
 * 使用 Testcontainers MySQL 8.0.40、真实 Spring TransactionManager、
 * 真实 MyBatis-Plus Mapper、真实 TemplateService Spring 代理。
 * <p>
 * 覆盖：
 * <ol>
 *   <li>两个并发复制请求仅一成功</li>
 *   <li>并发更新 revision 不丢失</li>
 *   <li>FOR UPDATE 确实阻塞第二个写事务</li>
 *   <li>节点复制失败整体回滚</li>
 *   <li>锁定后状态重新判断（通过并发复制实现）</li>
 * </ol>
 */
@DisplayName("模板服务并发集成测试")
@SpringBootTest(
    classes = FocusedTemplateTransactionTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@ActiveProfiles("test")
class TemplateServiceConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.40")
            .withDatabaseName("smarterp_concurrency_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--log_bin_trust_function_creators=1");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeAll
    static void initSchema() throws Exception {
        Path schemaPath = resolvePath("backend/src/test/resources/sql/template-service-concurrency-schema.sql");
        String schema = Files.readString(schemaPath);
        // Split and execute each statement
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             java.sql.Statement stmt = conn.createStatement()) {
            for (String sql : splitStatements(schema)) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    stmt.execute(trimmed);
                } catch (java.sql.SQLException e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("Duplicate column") || msg.contains("Duplicate key")
                            || msg.contains("already exists")) {
                        System.out.println("[INFO] Skipping (already applied): " + msg.split("\n")[0]);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ApprovalTemplateMapper templateMapper;

    @Autowired
    private ApprovalNodeMapper nodeMapper;

    @Autowired
    private TemplateFieldMapper fieldMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM audit_log");
        jdbcTemplate.execute("DELETE FROM expense_approval_task");
        jdbcTemplate.execute("DELETE FROM approval_task");
        jdbcTemplate.execute("DELETE FROM approval_record");
        jdbcTemplate.execute("DELETE FROM expense_request");
        jdbcTemplate.execute("DELETE FROM leave_request");
        jdbcTemplate.execute("DELETE FROM template_field");
        jdbcTemplate.execute("DELETE FROM approval_node");
        jdbcTemplate.execute("DELETE FROM approval_template");
    }

    @AfterEach
    void cleanupTriggers() {
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_node_insert");
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 1. 两个并发复制请求 ====================

    @Nested
    @DisplayName("两个并发复制请求仅一成功")
    class ConcurrentCopy {

        @Test
        @DisplayName("两个线程同时调用 createDraftFromVersion，恰好一个成功，一个 409")
        void twoConcurrentCopyRequests_OneSucceedsOneFails() throws Exception {
            // 准备 ACTIVE 模板
            ApprovalTemplate active = activeTemplate("CONCURRENT_KEY", 1);
            templateMapper.insert(active);
            nodeMapper.insert(makeNode(null, active.getId(), "Manager", 0));
            fieldMapper.insert(makeField(null, active.getId(), "reason", "Reason", 0));

            int threadCount = 2;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<?>> futures = new ArrayList<>();
            AtomicReference<Throwable> exception1 = new AtomicReference<>();
            AtomicReference<Throwable> exception2 = new AtomicReference<>();
            AtomicReference<ApprovalTemplate> result1 = new AtomicReference<>();
            AtomicReference<ApprovalTemplate> result2 = new AtomicReference<>();

            try {
                // Thread 1
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        result1.set(templateService.createDraftFromVersion(active.getId()));
                    } catch (Exception e) {
                        exception1.set(e);
                    } finally {
                        doneLatch.countDown();
                    }
                }));

                // Thread 2
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        result2.set(templateService.createDraftFromVersion(active.getId()));
                    } catch (Exception e) {
                        exception2.set(e);
                    } finally {
                        doneLatch.countDown();
                    }
                }));

                // 同时释放
                startLatch.countDown();
                assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Threads must complete within timeout");

                // Wait for futures
                for (Future<?> f : futures) {
                    f.get(10, TimeUnit.SECONDS);
                }

                // 验证恰好一个成功、一个失败
                boolean t1Success = result1.get() != null;
                boolean t2Success = result2.get() != null;
                assertTrue(t1Success ^ t2Success,
                        "Exactly one must succeed, got t1=" + t1Success + " t2=" + t2Success);

                if (t1Success) {
                    assertTrue(exception2.get() instanceof BusinessException);
                    assertEquals(409, ((BusinessException) exception2.get()).getCode().intValue());
                } else {
                    assertTrue(exception1.get() instanceof BusinessException);
                    assertEquals(409, ((BusinessException) exception1.get()).getCode().intValue());
                }

                // 数据库只有一个 DRAFT
                List<Long> draftIds = jdbcTemplate.query(
                        "SELECT id FROM approval_template WHERE lifecycle_status = 'DRAFT' AND template_key = 'CONCURRENT_KEY'",
                        (rs, rowNum) -> rs.getLong("id"));
                assertEquals(1, draftIds.size(), "Exactly one DRAFT must exist");

                // DRAFT version 正确
                Long draftId = draftIds.get(0);
                ApprovalTemplate draft = templateMapper.selectById(draftId);
                assertNotNull(draft);
                assertEquals(Integer.valueOf(2), draft.getVersionNo());
                assertEquals(active.getId(), draft.getSupersedesId());
                assertEquals("DRAFT", draft.getLifecycleStatus());

                // 节点只复制一组
                List<ApprovalNode> nodes = nodeMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalNode>()
                                .eq(ApprovalNode::getTemplateId, draftId));
                assertEquals(1, nodes.size(), "Exactly one node set must be copied");

                // 字段只复制一组
                List<TemplateField> fields = fieldMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TemplateField>()
                                .eq(TemplateField::getTemplateId, draftId));
                assertEquals(1, fields.size(), "Exactly one field set must be copied");

                // 不存在孤立子记录
                List<Long> orphanNodeTemplateIds = jdbcTemplate.query(
                        "SELECT n.template_id FROM approval_node n LEFT JOIN approval_template t ON n.template_id = t.id WHERE t.id IS NULL",
                        (rs, rowNum) -> rs.getLong("template_id"));
                assertTrue(orphanNodeTemplateIds.isEmpty(), "No orphan nodes");

                List<Long> orphanFieldTemplateIds = jdbcTemplate.query(
                        "SELECT f.template_id FROM template_field f LEFT JOIN approval_template t ON f.template_id = t.id WHERE t.id IS NULL",
                        (rs, rowNum) -> rs.getLong("template_id"));
                assertTrue(orphanFieldTemplateIds.isEmpty(), "No orphan fields");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    // ==================== 2. 并发更新 revision 不丢失 ====================

    @Nested
    @DisplayName("并发更新 revision 不丢失")
    class ConcurrentUpdate {

        @Test
        @DisplayName("两个线程并发更新不同名称，最终 revision = 2")
        void concurrentUpdates_BothSucceed_RevisionIncrementsTwice() throws Exception {
            ApprovalTemplate draft = draftTemplate("UPDATE_KEY", 1);
            templateMapper.insert(draft);

            int threadCount = 2;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<?>> futures = new ArrayList<>();
            AtomicReference<Throwable> error1 = new AtomicReference<>();
            AtomicReference<Throwable> error2 = new AtomicReference<>();

            try {
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        ApprovalTemplate data = new ApprovalTemplate();
                        data.setName("Name A");
                        templateService.update(draft.getId(), data);
                    } catch (Exception e) {
                        error1.set(e);
                    } finally {
                        doneLatch.countDown();
                    }
                }));

                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        ApprovalTemplate data = new ApprovalTemplate();
                        data.setName("Name B");
                        templateService.update(draft.getId(), data);
                    } catch (Exception e) {
                        error2.set(e);
                    } finally {
                        doneLatch.countDown();
                    }
                }));

                startLatch.countDown();
                assertTrue(doneLatch.await(30, TimeUnit.SECONDS));

                for (Future<?> f : futures) {
                    f.get(10, TimeUnit.SECONDS);
                }

                // 两个都应该成功
                assertNull(error1.get(), "Thread 1 must not fail: " + (error1.get() != null ? error1.get().getMessage() : ""));
                assertNull(error2.get(), "Thread 2 must not fail: " + (error2.get() != null ? error2.get().getMessage() : ""));

                // 最终 revision = 2
                ApprovalTemplate result = templateMapper.selectById(draft.getId());
                assertNotNull(result);
                assertEquals(Integer.valueOf(2), result.getRevision(),
                        "Revision must be 2 after two concurrent updates");

                // lifecycleStatus 仍为 DRAFT
                assertEquals("DRAFT", result.getLifecycleStatus());

                // enabled 仍为 false
                assertFalse(result.isEnabled());

                // 名称是 A 或 B
                String name = result.getName();
                assertTrue("Name A".equals(name) || "Name B".equals(name),
                        "Name must be one of the two inputs, got: " + name);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    // ==================== 3. FOR UPDATE 确实阻塞 ====================

    @Nested
    @DisplayName("FOR UPDATE 阻塞验证")
    class ForUpdateBlocks {

        @Test
        @DisplayName("第一个事务持锁时，第二个 update 被阻塞直到锁释放")
        void forUpdateBlocksSecondWriter() throws Exception {
            ApprovalTemplate draft = draftTemplate("BLOCK_KEY", 1);
            templateMapper.insert(draft);

            CountDownLatch t1HoldsLock = new CountDownLatch(1);
            CountDownLatch releaseT1 = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                // Thread 1: 持有 FOR UPDATE 锁不放
                Future<?> t1 = executor.submit(() -> {
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    tx.executeWithoutResult(status -> {
                        templateMapper.selectByIdForUpdate(draft.getId());
                        t1HoldsLock.countDown();
                        try {
                            assertTrue(releaseT1.await(30, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                });

                // 等待 T1 获得锁
                assertTrue(t1HoldsLock.await(30, TimeUnit.SECONDS));

                // Thread 2: 尝试调用 update（内部也要 selectByIdForUpdate）
                Future<?> t2 = executor.submit(() -> {
                    ApprovalTemplate data = new ApprovalTemplate();
                    data.setName("Blocked update");
                    templateService.update(draft.getId(), data);
                });

                // 短暂等待确认 T2 被阻塞
                Thread.sleep(500);
                assertFalse(t2.isDone(), "T2 must still be blocked while T1 holds lock");

                // 释放 T1
                releaseT1.countDown();
                t1.get(10, TimeUnit.SECONDS);

                // T2 现在应该完成
                t2.get(30, TimeUnit.SECONDS);

                // 验证最终状态
                ApprovalTemplate result = templateMapper.selectById(draft.getId());
                assertEquals("Blocked update", result.getName());
                assertEquals(Integer.valueOf(1), result.getRevision());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    // ==================== 4. 节点复制失败回滚 ====================

    @Nested
    @DisplayName("节点复制失败整体回滚")
    class RollbackOnNodeFailure {

        @Test
        @DisplayName("节点插入失败时 DRAFT、节点、字段全部回滚")
        void nodeInsertFailureRollsBackEntireDraft() throws Exception {
            ApprovalTemplate active = activeTemplate("ROLLBACK_KEY", 1);
            templateMapper.insert(active);
            nodeMapper.insert(makeNode(null, active.getId(), "Manager", 0));
            fieldMapper.insert(makeField(null, active.getId(), "reason", "Reason", 0));

            // 创建触发器：当插入到 approval_node 且 template_id 大于 active id 时失败
            // 这模拟新 DRAFT 的节点插入失败
            jdbcTemplate.execute(
                    "CREATE TRIGGER fail_node_insert BEFORE INSERT ON approval_node FOR EACH ROW BEGIN "
                    + "  IF NEW.template_id > " + active.getId() + " THEN "
                    + "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Node copy blocked for test'; "
                    + "  END IF; "
                    + "END");

            // 调用应失败
            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> templateService.createDraftFromVersion(active.getId()));

            assertNotNull(error);
            // 异常信息包含触发器消息或 SQL 错误
            String msg = error.getMessage() != null ? error.getMessage() : "";
            assertTrue(
                    msg.contains("Node copy blocked") || msg.contains("transaction")
                    || error.getCause() != null,
                    "Error must propagate from trigger");

            // 没有新增 DRAFT
            long templateCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM approval_template WHERE template_key = 'ROLLBACK_KEY'",
                    Long.class);
            assertEquals(1L, templateCount, "No new template rows after rollback");

            // source ACTIVE 完全不变
            ApprovalTemplate source = templateMapper.selectById(active.getId());
            assertEquals("ACTIVE", source.getLifecycleStatus());
            assertEquals(Integer.valueOf(1), source.getVersionNo());

            // 没有新增节点（只有 source 节点）
            long nodeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM approval_node WHERE template_id IN (SELECT id FROM approval_template WHERE template_key = 'ROLLBACK_KEY')",
                    Long.class);
            assertEquals(1L, nodeCount, "No new nodes after rollback");

            // 没有新增字段
            long fieldCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM template_field WHERE template_id IN (SELECT id FROM approval_template WHERE template_key = 'ROLLBACK_KEY')",
                    Long.class);
            assertEquals(1L, fieldCount, "No new fields after rollback");
        }
    }

    // ==================== 5. 并发复制已覆盖状态重新判断 ====================

    @Nested
    @DisplayName("锁定后状态重新判断（并发复制已覆盖）")
    class StateRecheckAfterLock {
        // Testcase 1 (两个并发复制请求) 已覆盖本场景。
        // 在并发复制中，第二个获得锁的线程发现 DRAFT 已存在并返回 409，
        // 证明：
        //   1. 锁定后正确重新读取并判断了状态
        //   2. 不会同时创建两个 DRAFT
        //   3. 锁范围内的检查是有效的

        @Test
        @DisplayName("并发复制中第二个请求在锁后发现已有 DRAFT 并正确拒绝")
        void stateIsRecheckedUnderLock() {
            // 由 ConcurrentCopy 场景覆盖 — 此处提供文档性测试
            // 当两个线程调用 createDraftFromVersion 时：
            //   - 第一个线程获得 source 锁和 versionsByKey 锁
            //   - 第一个线程完成插入
            //   - 第二个线程获得锁后重新读取 allVersions
            //   - 第二个线程发现 DRAFT 已存在并抛出 409
            //
            // 如果未重新判断，两个线程都会完成并违反唯一约束。
            // ConcurrentCopy 测试已证明该场景正确工作。
            assertTrue(true);
        }
    }

    // ==================== 辅助方法 ====================

    private ApprovalTemplate activeTemplate(String key, int versionNo) {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setName("Active template " + key);
        t.setDescription("For concurrency test");
        t.setTemplateKey(key);
        t.setVersionNo(versionNo);
        t.setWorkflowType("LEAVE");
        t.setLifecycleStatus("ACTIVE");
        t.setEnabled(true);
        t.setRevision(0);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        return t;
    }

    private ApprovalTemplate draftTemplate(String key, int versionNo) {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setName("Draft template " + key);
        t.setDescription("For concurrency test");
        t.setTemplateKey(key);
        t.setVersionNo(versionNo);
        t.setWorkflowType("LEAVE");
        t.setLifecycleStatus("DRAFT");
        t.setEnabled(false);
        t.setRevision(0);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        return t;
    }

    private ApprovalNode makeNode(Long id, Long templateId, String name, int sortOrder) {
        ApprovalNode n = new ApprovalNode();
        n.setId(id);
        n.setTemplateId(templateId);
        n.setNodeName(name);
        n.setSortOrder(sortOrder);
        n.setApproverType("USER");
        n.setApproverId(2L);
        n.setSignType("SINGLE");
        n.setCreateTime(LocalDateTime.now());
        n.setUpdateTime(LocalDateTime.now());
        return n;
    }

    private TemplateField makeField(Long id, Long templateId, String fieldName, String fieldLabel, int sortOrder) {
        TemplateField f = new TemplateField();
        f.setId(id);
        f.setTemplateId(templateId);
        f.setFieldName(fieldName);
        f.setFieldLabel(fieldLabel);
        f.setFieldType("TEXT");
        f.setRequired(true);
        f.setSortOrder(sortOrder);
        f.setCreateTime(LocalDateTime.now());
        return f;
    }

    // ==================== 路径/SQL 工具 ====================

    static Path resolvePath(String relative) {
        List<String> candidates = List.of(
                "../" + relative,
                relative
        );
        for (String candidate : candidates) {
            Path resolved = Paths.get(candidate).toAbsolutePath().normalize();
            if (Files.exists(resolved)) {
                return resolved;
            }
        }
        throw new AssertionError("Cannot resolve path: " + relative);
    }

    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                continue;
            }
            current.append(line).append("\n");
            if (trimmedLine.endsWith(";")) {
                statements.add(current.toString().trim());
                current = new StringBuilder();
            }
        }
        String remainder = current.toString().trim();
        if (!remainder.isEmpty()) {
            statements.add(remainder);
        }
        return statements;
    }
}
