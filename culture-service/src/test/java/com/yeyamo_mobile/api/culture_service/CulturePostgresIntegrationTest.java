package com.yeyamo_mobile.api.culture_service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.yeyamo_mobile.shared.country.CountryConfigClient;

@Tag("integration")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_TESTCONTAINERS", matches = "true")
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.task.scheduling.enabled=false",
        "jwt.secret=test-secret-that-is-long-enough-for-hmac-256"
})
class CulturePostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired DataSource dataSource;
    @Autowired RestClient.Builder restClientBuilder;
    @Autowired CountryConfigClient countryConfigClient;

    @Test
    void flywayCreatesCultureSchema() throws Exception {
        try (var connection = dataSource.getConnection();
                var result = connection.createStatement().executeQuery(
                        "select exists(select 1 from information_schema.tables where table_name='culture_contents')")) {
            assertTrue(result.next() && result.getBoolean(1));
        }
    }

    @Test
    void contextProvidesCountryConfigClientAndRestClientBuilder() {
        assertTrue(restClientBuilder != null);
        assertTrue(countryConfigClient != null);
    }
}
