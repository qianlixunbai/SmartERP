package com.smartoa.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SafeAttachmentUrlValidator} 单元测试。
 *
 * <p>不启动 Spring Context，不连接数据库。</p>
 */
@DisplayName("SafeAttachmentUrlValidator 单元测试")
class SafeAttachmentUrlValidatorTest {

    private SafeAttachmentUrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SafeAttachmentUrlValidator();
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
            assertTrue(validator.isValid(value, null));
        }

        @Test
        @DisplayName("HTTPS URL → 合法")
        void httpsUrl() {
            assertTrue(validator.isValid("https://example.com/receipts/2026/001.pdf", null));
        }

        @Test
        @DisplayName("HTTP localhost → 合法")
        void httpLocalhost() {
            assertTrue(validator.isValid("http://localhost:3000/uploads/test.pdf", null));
        }

        @Test
        @DisplayName("带端口 → 合法")
        void withPort() {
            assertTrue(validator.isValid("https://example.com:8080/path/file.pdf", null));
        }

        @Test
        @DisplayName("带 query → 合法")
        void withQuery() {
            assertTrue(validator.isValid("https://cdn.example.com/a.png?token=abc", null));
        }

        @Test
        @DisplayName("带 fragment → 合法")
        void withFragment() {
            assertTrue(validator.isValid("https://example.com/path/file.pdf#page=1", null));
        }

        @Test
        @DisplayName("scheme 大写 HTTPS → 合法")
        void uppercaseScheme() {
            assertTrue(validator.isValid("HTTPS://example.com/a.pdf", null));
        }

        @Test
        @DisplayName("scheme 混合大小写 Https → 合法")
        void mixedCaseScheme() {
            assertTrue(validator.isValid("Https://example.com/a.pdf", null));
        }
    }

    // ======================== 非法值 ========================

    @Nested
    @DisplayName("非法值")
    class InvalidUrls {

        @Test
        @DisplayName("纯空格 → 非法")
        void onlySpaces() {
            assertFalse(validator.isValid("   ", null));
        }

        @Test
        @DisplayName("前导空格 → 非法")
        void leadingSpace() {
            assertFalse(validator.isValid(" https://example.com/a.pdf", null));
        }

        @Test
        @DisplayName("尾随空格 → 非法")
        void trailingSpace() {
            assertFalse(validator.isValid("https://example.com/a.pdf ", null));
        }

        @Test
        @DisplayName("javascript: → 非法")
        void javascript() {
            assertFalse(validator.isValid("javascript:alert(1)", null));
        }

        @Test
        @DisplayName("JAVASCRIPT: → 非法")
        void uppercaseJavascript() {
            assertFalse(validator.isValid("JAVASCRIPT:alert(1)", null));
        }

        @Test
        @DisplayName("data: → 非法")
        void dataUri() {
            assertFalse(validator.isValid("data:text/plain,test", null));
        }

        @Test
        @DisplayName("file: → 非法")
        void fileUri() {
            assertFalse(validator.isValid("file:///tmp/a.pdf", null));
        }

        @Test
        @DisplayName("ftp: → 非法")
        void ftpUri() {
            assertFalse(validator.isValid("ftp://example.com/a.pdf", null));
        }

        @Test
        @DisplayName("jar: → 非法")
        void jarUri() {
            assertFalse(validator.isValid("jar:https://example.com/a.jar!/x", null));
        }

        @Test
        @DisplayName("mailto: → 非法")
        void mailtoUri() {
            assertFalse(validator.isValid("mailto:user@example.com", null));
        }

        @Test
        @DisplayName("vbscript: → 非法")
        void vbscriptUri() {
            assertFalse(validator.isValid("vbscript:msgbox(1)", null));
        }

        @Test
        @DisplayName("protocol-relative //evil.example.com/a.pdf → 非法")
        void protocolRelative() {
            assertFalse(validator.isValid("//evil.example.com/a.pdf", null));
        }

        @Test
        @DisplayName("根相对路径 /example/file.pdf → 非法")
        void rootRelative() {
            assertFalse(validator.isValid("/example/file.pdf", null));
        }

        @Test
        @DisplayName("普通相对路径 uploads/file.pdf → 非法")
        void relativePath() {
            assertFalse(validator.isValid("uploads/file.pdf", null));
        }

        @Test
        @DisplayName("无 scheme 域名 example.com/file.pdf → 非法")
        void noScheme() {
            assertFalse(validator.isValid("example.com/file.pdf", null));
        }

        @Test
        @DisplayName("缺少 host（https://）→ 非法")
        void missingHost() {
            assertFalse(validator.isValid("https://", null));
        }

        @Test
        @DisplayName("userInfo https://user:pass@example.com/a.pdf → 非法")
        void withUserInfo() {
            assertFalse(validator.isValid("https://user:pass@example.com/a.pdf", null));
        }

        @Test
        @DisplayName("反斜杠 https:\\example.com\\a.pdf → 非法")
        void backslash() {
            assertFalse(validator.isValid("https:\\example.com\\a.pdf", null));
        }

        @Test
        @DisplayName("包含 CR → 非法")
        void withCarriageReturn() {
            assertFalse(validator.isValid("https://example.com/a\r.pdf", null));
        }

        @Test
        @DisplayName("包含 LF → 非法")
        void withLineFeed() {
            assertFalse(validator.isValid("https://example.com/a\n.pdf", null));
        }

        @Test
        @DisplayName("包含 TAB → 非法")
        void withTab() {
            assertFalse(validator.isValid("https://example.com/a\t.pdf", null));
        }

        @Test
        @DisplayName("畸形 URI → 非法")
        void malformedUri() {
            assertFalse(validator.isValid("https://[invalid", null));
        }
    }
}
