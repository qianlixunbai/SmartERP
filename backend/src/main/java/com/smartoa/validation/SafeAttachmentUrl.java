package com.smartoa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验附件 URL 仅允许 HTTP 或 HTTPS 协议。
 *
 * <p>null 和空字符串视为合法（字段可选）。
 * 非空值必须通过 {@link SafeAttachmentUrlValidator} 的结构化解析。</p>
 */
@Documented
@Constraint(validatedBy = SafeAttachmentUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeAttachmentUrl {

    String message() default "附件URL仅允许有效的HTTP或HTTPS地址";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
