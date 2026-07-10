package com.smartoa.integration;

import com.smartoa.service.TemplateService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 最小 Spring Context，仅用于模板服务并发集成测试。
 * <p>
 * 只启用 DataSource、TransactionManager、MyBatis-Plus
 * 和 TemplateService，不加载 Web、Security、Controller、Scheduler
 * 或其他 Service。
 */
@Configuration
@EnableAutoConfiguration
@EnableTransactionManagement
@MapperScan("com.smartoa.mapper")
@Import(TemplateService.class)
public class FocusedTemplateTransactionTestApplication {
}
