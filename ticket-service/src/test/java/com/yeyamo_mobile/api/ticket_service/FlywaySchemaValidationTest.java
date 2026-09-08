package com.yeyamo_mobile.api.ticket_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Validates the production startup order: Flyway creates the empty schema,
 * then Hibernate validates the canonical UUID persistence model.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
class FlywaySchemaValidationTest {

    @Test
    void applicationContextStartsAfterFlywayMigratesTheUuidSchema() {
        // Context startup is the assertion: Flyway and Hibernate must both succeed.
    }
}
