package com.smartoa.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "jwt.secret 不能为空，请设置 SMARTERP_JWT_SECRET 环境变量")
    @Size(min = 32, message = "jwt.secret 长度不能少于 32 个字符")
    private String secret;

    private long expiration;
}
