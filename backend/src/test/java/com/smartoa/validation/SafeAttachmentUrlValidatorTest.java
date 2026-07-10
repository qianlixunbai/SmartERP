package com.smartoa.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SafeAttachmentUrl} 注解通过 Jakarta Bean Validation 验证测试。
 *
 * <p>不启动 Spring Context，不连接数据库。</p>
 */
@DisplayName("SafeAttachmentUrl Jakarta Validator 测试")
class SafeAttachmentUrlValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    /**
     * 测试载体，仅包含 receiptUrl 字段。
     */
    private static class AttachmentUrlFixture {

        @SafeAttachmentUrl
        private String receiptUrl;

        AttachmentUrlFixture(String receiptUrl) {
            this.receiptUrl = receiptUrl;
        }
    }

    // ======================== 有效值 ========================

    @Nested
    @DisplayName("有效值")
    class ValidUrls {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {""})
        @DisplayName("null 和空字符串 → 合法")
        void nullAndEmpty(String value) {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture(value));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("HTTPS URL → 合法")
        void httpsUrl() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("https://example.com/receipts/2026/001.pdf"));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("HTTP localhost → 合法")
        void httpLocalhost() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("http://localhost:3000/uploads/test.pdf"));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("带端口 → 合法")
        void withPort() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("https://example.com:8080/path/file.pdf"));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("带 query → 合法")
        void withQuery() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("https://cdn.example.com/a.png?token=abc"));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("带 fragment → 合法")
        void withFragment() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("https://example.com/path/file.pdf#page=1"));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("scheme 大写 HTTPS → 合法")
        void uppercaseScheme() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("HTTPS://example.com/a.pdf"));
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("scheme 混合大小写 Https → 合法")
        void mixedCaseScheme() {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture("Https://example.com/a.pdf"));
            assertTrue(violations.isEmpty());
        }
    }

    // ======================== 非法值 ========================

    @Nested
    @DisplayName("非法值")
    class InvalidUrls {

        @Test
        @DisplayName("纯空格 → 非法")
        void onlySpaces() {
            assertViolation("   ");
        }

        @Test
        @DisplayName("前导空格 → 非法")
        void leadingSpace() {
            assertViolation(" https://example.com/a.pdf");
        }

        @Test
        @DisplayName("尾随空格 → 非法")
        void trailingSpace() {
            assertViolation("https://example.com/a.pdf ");
        }

        @Test
        @DisplayName("javascript: → 非法")
        void javascript() {
            assertViolation("javascript:alert(1)");
        }

        @Test
        @DisplayName("JAVASCRIPT: → 非法")
        void uppercaseJavascript() {
            assertViolation("JAVASCRIPT:alert(1)");
        }

        @Test
        @DisplayName("data: → 非法")
        void dataUri() {
            assertViolation("data:text/plain,test");
        }

        @Test
        @DisplayName("file: → 非法")
        void fileUri() {
            assertViolation("file:///tmp/a.pdf");
        }

        @Test
        @DisplayName("ftp: → 非法")
        void ftpUri() {
            assertViolation("ftp://example.com/a.pdf");
        }

        @Test
        @DisplayName("jar: → 非法")
        void jarUri() {
            assertViolation("jar:https://example.com/a.jar!/x");
        }

        @Test
        @DisplayName("mailto: → 非法")
        void mailtoUri() {
            assertViolation("mailto:user@example.com");
        }

        @Test
        @DisplayName("vbscript: → 非法")
        void vbscriptUri() {
            assertViolation("vbscript:msgbox(1)");
        }

        @Test
        @DisplayName("protocol-relative //evil.example.com/a.pdf → 非法")
        void protocolRelative() {
            assertViolation("//evil.example.com/a.pdf");
        }

        @Test
        @DisplayName("根相对路径 /example/file.pdf → 非法")
        void rootRelative() {
            assertViolation("/example/file.pdf");
        }

        @Test
        @DisplayName("普通相对路径 uploads/file.pdf → 非法")
        void relativePath() {
            assertViolation("uploads/file.pdf");
        }

        @Test
        @DisplayName("无 scheme 域名 example.com/file.pdf → 非法")
        void noScheme() {
            assertViolation("example.com/file.pdf");
        }

        @Test
        @DisplayName("缺少 host（https://）→ 非法")
        void missingHost() {
            assertViolation("https://");
        }

        @Test
        @DisplayName("userInfo https://user:pass@example.com/a.pdf → 非法")
        void withUserInfo() {
            assertViolation("https://user:pass@example.com/a.pdf");
        }

        @Test
        @DisplayName("反斜杠 https:\\example.com\\a.pdf → 非法")
        void backslash() {
            assertViolation("https:\\example.com\\a.pdf");
        }

        @Test
        @DisplayName("包含 CR → 非法")
        void withCarriageReturn() {
            assertViolation("https://example.com/a\r.pdf");
        }

        @Test
        @DisplayName("包含 LF → 非法")
        void withLineFeed() {
            assertViolation("https://example.com/a\n.pdf");
        }

        @Test
        @DisplayName("包含 TAB → 非法")
        void withTab() {
            assertViolation("https://example.com/a\t.pdf");
        }

        @Test
        @DisplayName("畸形 URI → 非法")
        void malformedUri() {
            assertViolation("https://[invalid");
        }

        /**
         * 通用断言方法：验证非法值产生正确的 violation。
         */
        private void assertViolation(String value) {
            Set<ConstraintViolation<AttachmentUrlFixture>> violations =
                    validator.validate(new AttachmentUrlFixture(value));

            assertEquals(1, violations.size(), "应恰好产生1个 violation");

            ConstraintViolation<?> violation = violations.iterator().next();

            assertEquals(
                    "附件URL仅允许有效的HTTP或HTTPS地址",
                    violation.getMessage(),
                    "violation message 不匹配"
            );

            assertEquals(
                    "receiptUrl",
                    violation.getPropertyPath().toString(),
                    "property path 不匹配"
            );
        }
    }
}
