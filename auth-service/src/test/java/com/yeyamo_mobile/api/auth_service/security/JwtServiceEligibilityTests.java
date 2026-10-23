package com.yeyamo_mobile.api.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.auth_service.models.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceEligibilityTests {

    private static final String SECRET = "test-secret-for-jwt-eligibility-with-at-least-32-bytes";
    private static final String ISSUER = "https://auth.yeyamo.test";
    private static final String AUDIENCE = "yeyamo-api";

    @Test
    void currentVerifiedEmailAccessTokenIsValid() {
        JwtService service = service();
        User user = user("user@example.com", Instant.now());

        assertTrue(service.isTokenValid(service.generateAccessToken(user)));
    }

    @Test
    void currentUnverifiedEmailAccessTokenIsRejected() {
        JwtService service = service();
        User user = user("user@example.com", null);

        assertFalse(service.isTokenValid(service.generateAccessToken(user)));
    }

    @Test
    void historicalEmailTokenWithoutEligibilityClaimsIsRejected() {
        String token = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim("email", "user@example.com")
                .issuedAt(java.util.Date.from(Instant.now()))
                .expiration(java.util.Date.from(Instant.now().plusSeconds(300)))
                .signWith(key())
                .compact();

        assertFalse(service().isTokenValid(token));
    }

    @Test
    void historicalPhoneOnlyTokenRemainsCompatible() {
        String token = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim("phone", "+237699123456")
                .issuedAt(java.util.Date.from(Instant.now()))
                .expiration(java.util.Date.from(Instant.now().plusSeconds(300)))
                .signWith(key())
                .compact();

        assertTrue(service().isTokenValid(token));
    }

    private JwtService service() {
        return new JwtService(SECRET, "", 1_800_000L, ISSUER, AUDIENCE, "test-key");
    }

    private User user(String email, Instant verifiedAt) {
        User user = new User();
        user.setId(42L);
        user.setEmail(email);
        user.setEmailVerifiedAt(verifiedAt);
        return user;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
