package com.smartoa.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模板版本化 Schema 迁移集成测试。
 * <p>
 * 使用 Testcontainers MySQL 8.0.40 和纯 JDBC，
 * 不启动 Spring Boot Context，不加载 MyBatis。
 * <p>
 * 执行真实 {@code docs/mysql-p9-template-versioning-expand.sql}
 * 并验证所有新列、生成列、唯一索引和旧数据完整性。
 */
@DisplayName("模板版本化 Schema 迁移集成测试")
class TemplateVersionSchemaMigrationIntegrationTest {

    static final String MYSQL_IMAGE = "mysql:8.0.40";

    static MySQLContainer<?> mysql;
    static Connection connection;

    @BeforeAll
    static void startContainer() throws Exception {
        mysql = new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("smarterp_test_tv")
                .withUsername("test")
                .withPassword("test");

        // Mount pre-schema init script
        Path preSchemaPath = resolvePath("backend/src/test/resources/sql/template-versioning-pre-schema.sql");
        mysql.withCopyFileToContainer(
                MountableFile.forHostPath(preSchemaPath),
                "/docker-entrypoint-initdb.d/01-pre-schema.sql"
        );

        mysql.start();

        // JDBC connection
        connection = DriverManager.getConnection(
                mysql.getJdbcUrl(),
                mysql.getUsername(),
                mysql.getPassword()
        );

        // Execute the real migration script ONCE before all tests
        executeSqlFile(resolvePath("docs/mysql-p9-template-versioning-expand.sql"));
    }

    @AfterAll
    static void stopContainer() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // ignore
        }
        if (mysql != null) {
            mysql.stop();
        }
    }

    // ---- 路径解析 ----

    /**
     * 依次查找文件：
     * <ol>
     *   <li>{@code ../<relative>}（从 backend 目录运行 Maven）</li>
     *   <li>{@code <relative>}（从仓库根目录或 IDE 运行）</li>
     * </ol>
     * 找不到时测试明确失败。
     */
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
        throw new AssertionError(
                "Cannot resolve path: " + relative
                + ". Tried: " + candidates
                + " from working dir: " + Paths.get("").toAbsolutePath()
        );
    }

    /**
     * 读取并执行 SQL 文件，按分号拆分语句。
     * 兼容 USE database、PREPARE/EXECUTE/DEALLOCATE 语句块。
     */
    static void executeSqlFile(Path path) throws Exception {
        String sql = Files.readString(path);
        for (String statement : splitStatements(sql)) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) continue;
            // Skip USE statements — we are already connected to the correct database
            if (trimmed.toUpperCase().startsWith("USE ")) continue;
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(trimmed);
            } catch (SQLException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                // 如果是 "already exists" 错误说明脚本已执行过，跳过
                if (msg.contains("Duplicate column")
                        || msg.contains("Duplicate key")
                        || msg.contains("already exists")) {
                    System.out.println("[INFO] Skipping (already applied): " + msg.split("\n")[0]);
                } else {
                    throw new AssertionError(
                            "SQL error: " + trimmed.substring(0, Math.min(100, trimmed.length()))
                            + " ... : " + msg, e);
                }
            }
        }
    }

    /**
     * 简单语句拆分：按分号分割，忽略以 -- 开头的行。
     * 处理 PREPARE/EXECUTE/DEALLOCATE 语句块（每个语句单独拆分）。
     */
    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : sql.split("\n")) {
            String trimmedLine = line.trim();

            // Skip pure comment lines and empty lines
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                continue;
            }

            current.append(line).append("\n");

            // Each statement ends at a semicolon (including PREPARE, EXECUTE, DEALLOCATE)
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

    // ====================================================================
    // 共享状态重置（在修改数据的测试中使用）
    // ====================================================================

    void resetTemplateLifecycle() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE approval_template SET lifecycle_status = NULL,"
                    + " template_key = NULL, version_no = NULL WHERE id IN (1, 2)");
            stmt.execute("DELETE FROM approval_template WHERE id > 2");
        }
    }

    // ---- 辅助查询方法 ----

    boolean columnExists(String table, String column) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                     + " WHERE TABLE_SCHEMA = DATABASE()"
                     + " AND TABLE_NAME = '" + table + "'"
                     + " AND COLUMN_NAME = '" + column + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    boolean columnIsNullable(String table, String column) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS"
                     + " WHERE TABLE_SCHEMA = DATABASE()"
                     + " AND TABLE_NAME = '" + table + "'"
                     + " AND COLUMN_NAME = '" + column + "'")) {
            rs.next();
            return "YES".equals(rs.getString(1));
        }
    }

    String getColumnDefault(String table, String column) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS"
                     + " WHERE TABLE_SCHEMA = DATABASE()"
                     + " AND TABLE_NAME = '" + table + "'"
                     + " AND COLUMN_NAME = '" + column + "'")) {
            rs.next();
            return rs.getString(1);
        }
    }

    boolean indexExists(String table, String indexName) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS"
                     + " WHERE TABLE_SCHEMA = DATABASE()"
                     + " AND TABLE_NAME = '" + table + "'"
                     + " AND INDEX_NAME = '" + indexName + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    long countRows(String table) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // ====================================================================
    // 1. 迁移成功执行
    // ====================================================================

    @Test
    @DisplayName("迁移脚本成功执行，所有新列存在")
    void shouldHaveAllNewColumns() throws Exception {
        // Verify approval_template new columns
        assertTrue(columnExists("approval_template", "template_key"), "template_key");
        assertTrue(columnExists("approval_template", "version_no"), "version_no");
        assertTrue(columnExists("approval_template", "workflow_type"), "workflow_type");
        assertTrue(columnExists("approval_template", "lifecycle_status"), "lifecycle_status");
        assertTrue(columnExists("approval_template", "published_at"), "published_at");
        assertTrue(columnExists("approval_template", "retired_at"), "retired_at");
        assertTrue(columnExists("approval_template", "supersedes_id"), "supersedes_id");
        assertTrue(columnExists("approval_template", "revision"), "revision");
        assertTrue(columnExists("approval_template", "active_slot"), "active_slot (generated)");
        assertTrue(columnExists("approval_template", "draft_slot"), "draft_slot (generated)");

        // Verify expense_request new column
        assertTrue(columnExists("expense_request", "template_id"), "expense_request.template_id");

        // Verify audit_log new column
        assertTrue(columnExists("audit_log", "node_id"), "audit_log.node_id");
    }

    // ====================================================================
    // 2. Nullable 约束
    // ====================================================================

    @Nested
    @DisplayName("Nullable 约束")
    class NullableConstraints {

        @Test
        @DisplayName("template_key/version_no/workflow_type/lifecycle_status 仍允许 NULL")
        void versionFieldsAllowNull() throws Exception {
            assertTrue(columnIsNullable("approval_template", "template_key"));
            assertTrue(columnIsNullable("approval_template", "version_no"));
            assertTrue(columnIsNullable("approval_template", "workflow_type"));
            assertTrue(columnIsNullable("approval_template", "lifecycle_status"));
        }

        @Test
        @DisplayName("expense_request.template_id 允许 NULL")
        void expenseTemplateIdNullable() throws Exception {
            assertTrue(columnIsNullable("expense_request", "template_id"));
        }

        @Test
        @DisplayName("audit_log.node_id 允许 NULL")
        void auditLogNodeIdNullable() throws Exception {
            assertTrue(columnIsNullable("audit_log", "node_id"));
        }
    }

    // ====================================================================
    // 3. revision 约束
    // ====================================================================

    @Nested
    @DisplayName("revision 约束")
    class RevisionConstraints {

        @Test
        @DisplayName("revision 为 NOT NULL，默认 0")
        void revisionNotNullDefaultZero() throws Exception {
            assertFalse(columnIsNullable("approval_template", "revision"),
                    "revision should be NOT NULL");
            assertEquals("0", getColumnDefault("approval_template", "revision"),
                    "revision default should be '0'");
        }

        @Test
        @DisplayName("既有行的 revision 为 0")
        void existingRowsRevisionIsZero() throws Exception {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, revision FROM approval_template")) {
                while (rs.next()) {
                    assertEquals(0, rs.getInt("revision"),
                            "Row id=" + rs.getLong("id") + " should have revision=0");
                }
            }
        }
    }

    // ====================================================================
    // 4. 旧数据完整性
    // ====================================================================

    @Nested
    @DisplayName("旧数据完整性")
    class LegacyDataIntegrity {

        @Test
        @DisplayName("所有迁移前 ID 完全不变")
        void allIdsUnchanged() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs;
                List<Long> ids = new ArrayList<>();

                rs = stmt.executeQuery("SELECT id FROM approval_template ORDER BY id");
                while (rs.next()) ids.add(rs.getLong("id"));
                assertEquals(List.of(1L, 2L), ids, "template IDs unchanged");

                ids.clear();
                rs = stmt.executeQuery("SELECT id FROM approval_node ORDER BY id");
                while (rs.next()) ids.add(rs.getLong("id"));
                assertEquals(List.of(1L, 2L), ids, "node IDs unchanged");

                ids.clear();
                rs = stmt.executeQuery("SELECT id FROM leave_request ORDER BY id");
                while (rs.next()) ids.add(rs.getLong("id"));
                assertEquals(List.of(100L, 101L), ids, "leave request IDs unchanged");

                ids.clear();
                rs = stmt.executeQuery("SELECT id FROM expense_request ORDER BY id");
                while (rs.next()) ids.add(rs.getLong("id"));
                assertEquals(List.of(200L, 201L), ids, "expense request IDs unchanged");

                ids.clear();
                rs = stmt.executeQuery("SELECT id FROM audit_log ORDER BY id");
                while (rs.next()) ids.add(rs.getLong("id"));
                assertEquals(List.of(1L, 2L), ids, "audit log IDs unchanged");
            }
        }

        @Test
        @DisplayName("模板 name/description/enabled 完全不变")
        void templateNameDescriptionEnabledUnchanged() throws Exception {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, name, description, enabled FROM approval_template ORDER BY id")) {
                rs.next();
                assertEquals(1L, rs.getLong("id"));
                assertEquals("Leave Request", rs.getString("name"));
                assertEquals("Employee leave approval template", rs.getString("description"));
                assertTrue(rs.getBoolean("enabled"));

                rs.next();
                assertEquals(2L, rs.getLong("id"));
                assertEquals("Expense Report", rs.getString("name"));
                assertFalse(rs.getBoolean("enabled"));
            }
        }

        @Test
        @DisplayName("请假单 templateId 完全不变")
        void leaveRequestTemplateIdUnchanged() throws Exception {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, template_id FROM leave_request ORDER BY id")) {
                rs.next();
                assertEquals(100L, rs.getLong("id"));
                assertEquals(1L, rs.getLong("template_id"));
                rs.next();
                assertEquals(101L, rs.getLong("id"));
                assertEquals(1L, rs.getLong("template_id"));
            }
        }

        @Test
        @DisplayName("经费单和审计日志仍存在")
        void expenseAndAuditStillExist() throws Exception {
            assertEquals(2, countRows("expense_request"));
            assertEquals(2, countRows("audit_log"));
        }
    }

    // ====================================================================
    // 5. 唯一约束
    // ====================================================================

    @Nested
    @DisplayName("唯一约束")
    class UniqueConstraints {

        @BeforeEach
        void setUp() throws Exception {
            resetTemplateLifecycle();
        }

        @Test
        @DisplayName("相同 (template_key, version_no) 无法重复")
        void duplicateTemplateKeyVersionNoFails() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'K1', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET template_key = 'K2', version_no = 1 WHERE id = 2");
            }

            SQLException ex = assertThrows(SQLException.class, () -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("INSERT INTO approval_template (name, template_key, version_no, revision)"
                            + " VALUES ('dup', 'K1', 1, 0)");
                }
            });
            assertTrue(
                    ex.getMessage() != null && (
                            ex.getMessage().contains("Duplicate")
                            || ex.getMessage().contains("duplicate")
                            || ex.getMessage().contains("uk_template_key_version")),
                    "Should get duplicate key error, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("同一 template_key 无法插入两个 ACTIVE")
        void twoActiveSameKeyFails() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'KA', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET template_key = 'KB', version_no = 1 WHERE id = 2");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'ACTIVE' WHERE id = 1");
            }

            SQLException ex = assertThrows(SQLException.class, () -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("INSERT INTO approval_template"
                            + " (name, template_key, version_no, lifecycle_status, revision)"
                            + " VALUES ('second_active', 'KA', 2, 'ACTIVE', 0)");
                }
            });
            assertTrue(
                    ex.getMessage() != null && (
                            ex.getMessage().contains("Duplicate")
                            || ex.getMessage().contains("uk_active_slot")),
                    "Should get duplicate key for uk_active_slot, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("同一 template_key 无法插入两个 DRAFT")
        void twoDraftSameKeyFails() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'KD', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET template_key = 'KX', version_no = 1 WHERE id = 2");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'DRAFT' WHERE id = 1");
            }

            SQLException ex = assertThrows(SQLException.class, () -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("INSERT INTO approval_template"
                            + " (name, template_key, version_no, lifecycle_status, revision)"
                            + " VALUES ('second_draft', 'KD', 2, 'DRAFT', 0)");
                }
            });
            assertTrue(
                    ex.getMessage() != null && (
                            ex.getMessage().contains("Duplicate")
                            || ex.getMessage().contains("uk_draft_slot")),
                    "Should get duplicate key for uk_draft_slot, got: " + ex.getMessage());
        }

        @Test
        @DisplayName("同一 template_key 可以有多个 RETIRED")
        void multipleRetiredSameKeyAllowed() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'KR', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET template_key = 'KY', version_no = 1 WHERE id = 2");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'RETIRED' WHERE id = 1");
            }

            assertDoesNotThrow(() -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("INSERT INTO approval_template"
                            + " (name, template_key, version_no, lifecycle_status, revision)"
                            + " VALUES ('retired_v2', 'KR', 2, 'RETIRED', 0)");
                    stmt.execute("INSERT INTO approval_template"
                            + " (name, template_key, version_no, lifecycle_status, revision)"
                            + " VALUES ('retired_v3', 'KR', 3, 'RETIRED', 0)");
                }
            });
        }

        @Test
        @DisplayName("不同 template_key 可以各有一个 ACTIVE")
        void differentKeyEachHasActive() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'CA', version_no = 1 WHERE id = 1");
                // Use version_no=2 for 'CB' row 2, then insert a fresh row with version_no=1
                stmt.execute("UPDATE approval_template SET template_key = 'CB', version_no = 2 WHERE id = 2");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'ACTIVE' WHERE id = 1");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'RETIRED' WHERE id = 2");
            }

            assertDoesNotThrow(() -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("INSERT INTO approval_template"
                            + " (name, template_key, version_no, lifecycle_status, revision)"
                            + " VALUES ('cb_active', 'CB', 1, 'ACTIVE', 0)");
                }
            });
        }

        @Test
        @DisplayName("现有 legacy 行（NULL template_key）在新唯一索引下可以共存")
        void legacyNullKeysCoexistUnderUniqueIndexes() throws Exception {
            // With template_key=NULL for existing rows (after reset),
            // MySQL treats NULLs as distinct in UNIQUE indexes
            assertEquals(2, countRows("approval_template"));

            assertDoesNotThrow(() -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("INSERT INTO approval_template (name, revision)"
                            + " VALUES ('third_null_key', 0)");
                }
            });
        }
    }

    // ====================================================================
    // 6. 生成列
    // ====================================================================

    @Nested
    @DisplayName("生成列")
    class GeneratedColumns {

        @BeforeEach
        void setUp() throws Exception {
            resetTemplateLifecycle();
        }

        @Test
        @DisplayName("active_slot 由数据库正确生成 — ACTIVE 时 = template_key")
        void activeSlotEqualsTemplateKeyWhenActive() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'GA', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET template_key = 'GX', version_no = 1 WHERE id = 2");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'ACTIVE' WHERE id = 1");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, template_key, lifecycle_status, active_slot FROM approval_template ORDER BY id")) {
                rs.next();
                assertEquals(1L, rs.getLong("id"));
                assertEquals("GA", rs.getString("template_key"));
                assertEquals("ACTIVE", rs.getString("lifecycle_status"));
                assertEquals("GA", rs.getString("active_slot"));

                rs.next();
                assertEquals(2L, rs.getLong("id"));
                assertNull(rs.getString("active_slot"));
            }
        }

        @Test
        @DisplayName("draft_slot 由数据库正确生成 — DRAFT 时 = template_key")
        void draftSlotEqualsTemplateKeyWhenDraft() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'GD', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET template_key = 'GY', version_no = 1 WHERE id = 2");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'DRAFT' WHERE id = 1");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, template_key, lifecycle_status, draft_slot FROM approval_template ORDER BY id")) {
                rs.next();
                assertEquals(1L, rs.getLong("id"));
                assertEquals("GD", rs.getString("template_key"));
                assertEquals("DRAFT", rs.getString("lifecycle_status"));
                assertEquals("GD", rs.getString("draft_slot"));

                rs.next();
                assertEquals(2L, rs.getLong("id"));
                assertNull(rs.getString("draft_slot"));
            }
        }

        @Test
        @DisplayName("RETIRED 时 active_slot 和 draft_slot 均为 NULL")
        void retiredSlotsAreNull() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE approval_template SET template_key = 'GR', version_no = 1 WHERE id = 1");
                stmt.execute("UPDATE approval_template SET lifecycle_status = 'RETIRED' WHERE id = 1");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT active_slot, draft_slot FROM approval_template WHERE id = 1")) {
                rs.next();
                assertNull(rs.getString("active_slot"));
                assertNull(rs.getString("draft_slot"));
            }
        }
    }

    // ====================================================================
    // 7. 无跨表 FK
    // ====================================================================

    @Nested
    @DisplayName("无跨表 FK")
    class NoCrossTableForeignKeys {

        @Test
        @DisplayName("没有添加要求历史数据完整的跨表 FK")
        void noNewCrossTableFKs() throws Exception {
            // expense_request.template_id has no FK
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE"
                         + " WHERE TABLE_SCHEMA = DATABASE()"
                         + " AND TABLE_NAME = 'expense_request'"
                         + " AND COLUMN_NAME = 'template_id'"
                         + " AND REFERENCED_TABLE_NAME IS NOT NULL")) {
                rs.next();
                assertEquals(0, rs.getInt(1), "expense_request.template_id should have no FK");
            }

            // audit_log.node_id has no FK
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE"
                         + " WHERE TABLE_SCHEMA = DATABASE()"
                         + " AND TABLE_NAME = 'audit_log'"
                         + " AND COLUMN_NAME = 'node_id'"
                         + " AND REFERENCED_TABLE_NAME IS NOT NULL")) {
                rs.next();
                assertEquals(0, rs.getInt(1), "audit_log.node_id should have no FK");
            }

            // approval_template.supersedes_id has no FK
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE"
                         + " WHERE TABLE_SCHEMA = DATABASE()"
                         + " AND TABLE_NAME = 'approval_template'"
                         + " AND COLUMN_NAME = 'supersedes_id'"
                         + " AND REFERENCED_TABLE_NAME IS NOT NULL")) {
                rs.next();
                assertEquals(0, rs.getInt(1), "approval_template.supersedes_id should have no FK");
            }
        }
    }

    // ====================================================================
    // 8. 索引存在性
    // ====================================================================

    @Nested
    @DisplayName("索引存在性")
    class IndexExistence {

        @Test
        @DisplayName("approval_template 新索引全部存在")
        void newTemplateIndexesExist() throws Exception {
            assertTrue(indexExists("approval_template", "uk_template_key_version"),
                    "uk_template_key_version");
            assertTrue(indexExists("approval_template", "uk_active_slot"),
                    "uk_active_slot");
            assertTrue(indexExists("approval_template", "uk_draft_slot"),
                    "uk_draft_slot");
            assertTrue(indexExists("approval_template", "idx_template_key_status"),
                    "idx_template_key_status");
            assertTrue(indexExists("approval_template", "idx_supersedes"),
                    "idx_supersedes");
        }

        @Test
        @DisplayName("expense_request idx_expense_template 存在")
        void expenseTemplateIndexExists() throws Exception {
            assertTrue(indexExists("expense_request", "idx_expense_template"));
        }

        @Test
        @DisplayName("audit_log idx_audit_node 存在")
        void auditNodeIndexExists() throws Exception {
            assertTrue(indexExists("audit_log", "idx_audit_node"));
        }

        @Test
        @DisplayName("leave_request idx_leave_template 存在")
        void leaveTemplateIndexExists() throws Exception {
            assertTrue(indexExists("leave_request", "idx_leave_template"));
        }
    }

    // ====================================================================
    // 9. enabled 字段保留
    // ====================================================================

    @Nested
    @DisplayName("enabled 字段保留")
    class EnabledColumnPreserved {

        @Test
        @DisplayName("enabled 列仍然存在")
        void enabledColumnStillExists() throws Exception {
            assertTrue(columnExists("approval_template", "enabled"),
                    "enabled column must still exist");
        }

        @Test
        @DisplayName("enabled 列可正常读写")
        void enabledColumnStillReadable() throws Exception {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, enabled FROM approval_template ORDER BY id")) {
                rs.next();
                assertEquals(1L, rs.getLong("id"));
                assertTrue(rs.getBoolean("enabled"));
                rs.next();
                assertEquals(2L, rs.getLong("id"));
                assertFalse(rs.getBoolean("enabled"));
            }
        }
    }
}
