package com.smartoa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * {@link SafeAttachmentUrl} 的校验器实现。
 *
 * <p>使用 {@link java.net.URI} 进行结构化解析，不访问网络。
 * null 和空字符串返回 true（字段可选），纯空格返回 false。</p>
 */
public class SafeAttachmentUrlValidator implements ConstraintValidator<SafeAttachmentUrl, String> {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 和空字符串：合法（字段可选）
        if (value == null || value.isEmpty()) {
            return true;
        }

        // 纯空格：拒绝
        if (value.trim().isEmpty()) {
            return false;
        }

        // 原始字符串必须等于 trim 后的字符串
        if (!value.equals(value.trim())) {
            return false;
        }

        // 不含控制字符
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c)) {
                return false;
            }
        }

        // 不含反斜杠
        if (value.indexOf('\\') >= 0) {
            return false;
        }

        // 使用 java.net.URI 进行结构化解析
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return false;
        }

        // 不得是 opaque URI（如 javascript:alert(1)）
        if (uri.isOpaque()) {
            return false;
        }

        // 必须是绝对 URI
        if (!uri.isAbsolute()) {
            return false;
        }

        // scheme 仅允许 http 或 https（大小写不敏感）
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            return false;
        }

        // host 非 null 且非空
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return false;
        }

        // userInfo 必须为 null（拒绝 https://user:pass@example.com/...）
        if (uri.getUserInfo() != null) {
            return false;
        }

        return true;
    }
}
