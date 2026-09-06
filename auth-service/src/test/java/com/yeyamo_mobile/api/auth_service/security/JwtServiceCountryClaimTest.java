package com.yeyamo_mobile.api.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.auth_service.models.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceCountryClaimTest {

    private static final String SECRET = "test-secret-for-country-claim-with-at-least-32-bytes";

    @Test
    void accessTokenContainsRegisteredAccountCountry() {
        JwtService service = new JwtService(SECRET, "", 1_800_000L,
                "https://auth.yeyamo.test", "yeyamo-api", "test-key");
        User user = new User();
        user.setId(42L);
        user.setEmail("user@yeyamo.test");
        user.setCountryCode("CM");

        String token = service.generateAccessToken(user);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        assertEquals("CM", Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().get("country", String.class));
    }
}
