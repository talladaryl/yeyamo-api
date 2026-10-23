package com.yeyamo.security.hardening;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAccountEligibilityValidatorTest {

    private final JwtAccountEligibilityValidator validator = new JwtAccountEligibilityValidator();

    @Test
    void rejectsLegacyEmailTokenWithoutVerificationProof() {
        Jwt jwt = jwt(Map.of("sub", "42", "email", "pending@example.com"));

        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void acceptsVerifiedActiveEmailToken() {
        Jwt jwt = jwt(Map.of("sub", "42", "email", "verified@example.com",
                "email_verified", true, "account_status", "ACTIVE"));

        assertFalse(validator.validate(jwt).hasErrors());
    }

    @Test
    void acceptsPhoneOnlyTokenWithoutEmailClaim() {
        Jwt jwt = jwt(Map.of("sub", "42", "account_status", "ACTIVE"));

        assertFalse(validator.validate(jwt).hasErrors());
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"), claims);
    }
}
