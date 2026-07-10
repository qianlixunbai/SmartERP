package com.smartoa.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtProperties 配置校验测试（无 Spring Context）")
class JwtPropertiesValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private JwtProperties props(String secret) {
        JwtProperties p = new JwtProperties();
        p.setSecret(secret);
        p.setExpiration(86400000L);
        return p;
    }

    @Test
    @DisplayName("secret 为 null 时校验应失败")
    void nullSecret_ShouldFail() {
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(props(null));
        assertFalse(violations.isEmpty(), "null secret 应触发校验失败");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("secret")));
    }

    @Test
    @DisplayName("secret 为空字符串时校验应失败")
    void emptySecret_ShouldFail() {
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(props(""));
        assertFalse(violations.isEmpty(), "空 secret 应触发校验失败");
    }

    @Test
    @DisplayName("secret 少于 32 字符时校验应失败")
    void shortSecret_ShouldFail() {
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(props("short-key"));
        assertFalse(violations.isEmpty(), "少于 32 字符的 secret 应触发校验失败");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("32")));
    }

    @Test
    @DisplayName("合法 32 字符 secret 校验应通过")
    void validSecret_ShouldPass() {
        String validKey = "a]bC3fGh1jKlMnOpQrStUvWxYz012345"; // exactly 32 chars
        assertEquals(32, validKey.length(), "测试前提：密钥长度应为 32");
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(props(validKey));
        assertTrue(violations.isEmpty(), "合法 secret 不应触发校验失败: " + violations);
    }

    @Test
    @DisplayName("secret 恰好 31 字符时校验应失败")
    void exactly31Chars_ShouldFail() {
        String key31 = "a]bC3fGh1jKlMnOpQrStUvWxYz01234"; // 31 chars
        assertEquals(31, key31.length(), "测试前提：密钥长度应为 31");
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(props(key31));
        assertFalse(violations.isEmpty(), "31 字符 secret 应触发校验失败");
    }
}
