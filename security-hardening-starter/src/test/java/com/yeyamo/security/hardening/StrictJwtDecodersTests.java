package com.yeyamo.security.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class StrictJwtDecodersTests {
    private static final String CURRENT = "current-secret-key-012345678901234567890123";
    private static final String PREVIOUS = "previous-secret-key-01234567890123456789012";
    private static final String ISSUER = "https://auth.yeyamo.test";
    private static final String AUDIENCE = "yeyamo-api";

    @Test
    void acceptsCurrentAndPreviousRotationKeys() throws Exception {
        var decoder = StrictJwtDecoders.create(CURRENT, PREVIOUS, "", ISSUER, AUDIENCE);

        assertEquals("42", decoder.decode(token(CURRENT, ISSUER, AUDIENCE, Instant.now().plusSeconds(60))).getSubject());
        assertEquals("42", decoder.decode(token(PREVIOUS, ISSUER, AUDIENCE, Instant.now().plusSeconds(60))).getSubject());
    }

    @Test
    void rejectsWrongAudienceAndExpiredTokens() throws Exception {
        var decoder = StrictJwtDecoders.create(CURRENT, "", "", ISSUER, AUDIENCE);

        assertThrows(JwtException.class,
                () -> decoder.decode(token(CURRENT, ISSUER, "another-api", Instant.now().plusSeconds(60))));
        assertThrows(JwtException.class,
                () -> decoder.decode(token(CURRENT, ISSUER, AUDIENCE, Instant.now().minusSeconds(120))));
    }

    private String token(String secret, String issuer, String audience, Instant expiration) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("42")
                .issuer(issuer)
                .audience(List.of(audience))
                .issueTime(Date.from(Instant.now().minusSeconds(1)))
                .expirationTime(Date.from(expiration))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
