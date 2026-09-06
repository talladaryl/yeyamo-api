package com.yeyamo_mobile.api.ticket_service;

import com.yeyamo_mobile.api.ticket_service.infrastructure.security.QrSigningKeyManager;
import com.yeyamo_mobile.api.ticket_service.infrastructure.security.QrTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test QR token security features
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TicketServiceTestConfiguration.class)
class QrTokenSecurityTest {
    
    @Autowired
    private QrTokenService qrTokenService;
    
    @Autowired
    private QrSigningKeyManager keyManager;
    
    @Test
    void testGenerateValidToken() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        QrTokenService.QrTokenData tokenData = qrTokenService.generateToken(ticketId, eventId, 365);
        
        assertThat(tokenData.token()).isNotNull();
        assertThat(tokenData.tokenId()).isNotNull();
        assertThat(tokenData.tokenHash()).isNotNull();
        assertThat(tokenData.keyId()).isNotNull();
        assertThat(tokenData.expiresAt()).isAfter(Instant.now());
    }
    
    @Test
    void testValidateValidToken() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        QrTokenService.QrTokenData tokenData = qrTokenService.generateToken(ticketId, eventId, 365);
        
        QrTokenService.TokenValidationResult result = qrTokenService.validateToken(tokenData.token());
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getTicketId()).isEqualTo(ticketId);
        assertThat(result.getEventId()).isEqualTo(eventId);
        assertThat(result.getTokenId()).isEqualTo(tokenData.tokenId());
    }
    
    @Test
    void testValidateTamperedToken() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        QrTokenService.QrTokenData tokenData = qrTokenService.generateToken(ticketId, eventId, 365);
        
        // Tamper with the token
        String tamperedToken = tokenData.token().substring(0, tokenData.token().length() - 10) + "XXXXXXXXXX";
        
        QrTokenService.TokenValidationResult result = qrTokenService.validateToken(tamperedToken);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isIn("Invalid signature", "Malformed token", "Validation failed");
    }
    
    @Test
    void testValidateExpiredToken() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        // Create an expired token (expires 1 day ago)
        String tokenId = UUID.randomUUID().toString();
        String keyId = keyManager.getCurrentKeyId();
        Instant now = Instant.now();
        Instant expiry = now.minus(1, ChronoUnit.DAYS);
        
        String expiredToken = Jwts.builder()
                .setId(tokenId)
                .setSubject(ticketId)
                .claim("eventId", eventId)
                .setIssuedAt(Date.from(now.minus(2, ChronoUnit.DAYS)))
                .setExpiration(Date.from(expiry))
                .setHeaderParam("kid", keyId)
                .signWith(keyManager.getCurrentPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
        
        QrTokenService.TokenValidationResult result = qrTokenService.validateToken(expiredToken);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("expired");
    }
    
    @Test
    void testValidateTokenWithWrongKey() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        // Create token signed with different key
        KeyPair wrongKeyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        
        String tokenId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plus(365, ChronoUnit.DAYS);
        
        String tokenWithWrongKey = Jwts.builder()
                .setId(tokenId)
                .setSubject(ticketId)
                .claim("eventId", eventId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .setHeaderParam("kid", "wrong-key-id")
                .signWith(wrongKeyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
        
        QrTokenService.TokenValidationResult result = qrTokenService.validateToken(tokenWithWrongKey);
        
        assertThat(result.isValid()).isFalse();
    }
    
    @Test
    void testTokenHashIsConsistent() {
        String token = "test-token-123";
        
        String hash1 = qrTokenService.hashToken(token);
        String hash2 = qrTokenService.hashToken(token);
        
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 produces 64 hex characters
    }
    
    @Test
    void testTokenDoesNotContainPersonalData() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        QrTokenService.QrTokenData tokenData = qrTokenService.generateToken(ticketId, eventId, 365);
        
        // Token should not contain user name, email, phone, etc.
        String token = tokenData.token();
        assertThat(token).doesNotContain("@");
        assertThat(token).doesNotContain("user");
        assertThat(token).doesNotContain("name");
        assertThat(token).doesNotContain("email");
        assertThat(token).doesNotContain("phone");
    }
}
