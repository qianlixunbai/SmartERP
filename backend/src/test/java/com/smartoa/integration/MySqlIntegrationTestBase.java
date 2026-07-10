package com.smartoa.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MySQL 集成测试基类。
 * <p>
 * 使用 Testcontainers 启动真实 MySQL 8.0 容器，
 * 通过 {@code @DynamicPropertySource} 注入 datasource 属性。
 * <p>
 * 静态容器保证整个测试类只启动一次 MySQL。
 * 如果 Docker 不可用，测试将明确失败（不会静默跳过）。
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
public abstract class MySqlIntegrationTestBase {

    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.40")
            .withDatabaseName("smarterp_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/stabilization-schema.sql");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
