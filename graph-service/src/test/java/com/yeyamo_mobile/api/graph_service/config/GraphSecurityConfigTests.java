package com.yeyamo_mobile.api.graph_service.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

class GraphSecurityConfigTests {

    private static final String SECRET = "graph-test-secret-with-at-least-32-bytes-for-jwt";
    private static final String ISSUER = "yeyamo-auth";
    private static final String AUDIENCE = "yeyamo-api";

    @Test
    void strictDecoderAcceptsVerifiedEmailToken() {
        assertDoesNotThrow(() -> decoder().decode(token("user@example.com", true, "ACTIVE")));
    }

    @Test
    void strictDecoderRejectsUnverifiedEmailToken() {
        assertThrows(JwtException.class, () -> decoder().decode(token("user@example.com", false, "ACTIVE")));
    }

    @Test
    void strictDecoderRejectsInactiveAccountToken() {
        assertThrows(JwtException.class, () -> decoder().decode(token("user@example.com", true, "SUSPENDED")));
    }

    @Test
    void strictDecoderKeepsHistoricalPhoneOnlyCompatibility() {
        assertDoesNotThrow(() -> decoder().decode(token(null, null, null)));
    }

    private JwtDecoder decoder() {
        return new GraphSecurityConfig().decoder(SECRET, "", "", ISSUER, AUDIENCE);
    }

    private String token(String email, Boolean verified, String status) {
        var claims = JwtClaimsSet.builder()
                .subject("42")
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (email != null) claims.claim("email", email);
        if (verified != null) claims.claim("email_verified", verified);
        if (status != null) claims.claim("account_status", status);
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }
}
