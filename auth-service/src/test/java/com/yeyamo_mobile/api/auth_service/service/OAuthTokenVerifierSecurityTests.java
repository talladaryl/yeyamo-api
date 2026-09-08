package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

class OAuthTokenVerifierSecurityTests {

    @Test
    void acceptsGoogleTokenWithVerifiedSignatureIssuerAndAudience() {
        JwtDecoder google = token -> jwt("https://accounts.google.com", List.of("yeyamo-google"), true);
        OAuthTokenVerifier verifier = new OAuthTokenVerifier("yeyamo-google", "yeyamo-apple", google, google);

        assertDoesNotThrow(() -> verifier.verify("google", "signed-token"));
    }

    @Test
    void rejectsInvalidSignatureAndWrongIssuer() {
        JwtDecoder invalidSignature = token -> { throw new org.springframework.security.oauth2.jwt.JwtException("invalid signature"); };
        OAuthTokenVerifier invalidVerifier = new OAuthTokenVerifier("yeyamo-google", "yeyamo-apple", invalidSignature, invalidSignature);
        assertEquals("GOOGLE_TOKEN_INVALID", assertThrows(ApiException.class,
                () -> invalidVerifier.verify("google", "forged-token")).getCode());

        JwtDecoder wrongIssuer = token -> jwt("https://attacker.example", List.of("yeyamo-google"), true);
        OAuthTokenVerifier issuerVerifier = new OAuthTokenVerifier("yeyamo-google", "yeyamo-apple", wrongIssuer, wrongIssuer);
        assertEquals("GOOGLE_ISSUER_INVALID", assertThrows(ApiException.class,
                () -> issuerVerifier.verify("google", "signed-token")).getCode());
    }

    @Test
    void rejectsTokenIssuedForAnotherOAuthClient() {
        JwtDecoder google = token -> jwt("https://accounts.google.com", List.of("attacker-client"), true);
        OAuthTokenVerifier verifier = new OAuthTokenVerifier("yeyamo-google", "yeyamo-apple", google, google);

        ApiException failure = assertThrows(ApiException.class, () -> verifier.verify("google", "signed-token"));

        assertEquals("GOOGLE_AUDIENCE_INVALID", failure.getCode());
    }

    @Test
    void rejectsUnverifiedEmailAndAmbiguousAuthorizedParty() {
        JwtDecoder unverified = token -> jwt("https://accounts.google.com", List.of("yeyamo-google"), false);
        OAuthTokenVerifier unverifiedVerifier = new OAuthTokenVerifier("yeyamo-google", "yeyamo-apple", unverified, unverified);
        assertEquals("GOOGLE_EMAIL_NOT_VERIFIED",
                assertThrows(ApiException.class, () -> unverifiedVerifier.verify("google", "signed-token")).getCode());

        JwtDecoder ambiguous = token -> Jwt.withTokenValue("signed-token")
                .header("alg", "RS256").subject("subject-1").issuer("https://accounts.google.com")
                .audience(List.of("yeyamo-google", "another-client")).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).claim("email", "test@example.com")
                .claim("email_verified", true).claim("azp", "another-client").build();
        OAuthTokenVerifier ambiguousVerifier = new OAuthTokenVerifier("yeyamo-google", "yeyamo-apple", ambiguous, ambiguous);
        assertEquals("OAUTH_AUTHORIZED_PARTY_INVALID",
                assertThrows(ApiException.class, () -> ambiguousVerifier.verify("google", "signed-token")).getCode());
    }

    @Test
    void startsWithoutOAuthClientIdsAndRejectsOnlyTheDisabledProviderCall() {
        JwtDecoder decoder = token -> jwt("https://accounts.google.com", List.of("client"), true);
        OAuthTokenVerifier verifier = new OAuthTokenVerifier("your-google-client-id", "", decoder, decoder);

        ApiException failure = assertThrows(
                ApiException.class,
                () -> verifier.verify("google", "signed-token"));

        assertEquals("OAUTH_PROVIDER_NOT_CONFIGURED", failure.getCode());
        assertEquals(503, failure.getStatus().value());
    }

    private Jwt jwt(String issuer, List<String> audience, boolean verified) {
        return Jwt.withTokenValue("signed-token").header("alg", "RS256").subject("subject-1")
                .issuer(issuer).audience(audience).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).claim("email", "test@example.com")
                .claim("email_verified", verified).build();
    }
}
