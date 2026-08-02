package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.security;

import com.yeyamo_mobile.api.ads_delivery_service.application.dto.TrackingTokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtTrackingTokenServiceTest {

    private JwtTrackingTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTrackingTokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-for-unit-tests");
    }

    @Test
    void shouldGenerateAndVerifyImpressionToken() {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        String userId = "USER123";
        Instant expiresAt = Instant.now().plusSeconds(300);

        // When
        String token = tokenService.generateImpressionToken(deliveryId, campaignId, userId, expiresAt);

        // Then
        assertNotNull(token);
        assertTrue(tokenService.verifyImpressionToken(token));
    }

    @Test
    void shouldGenerateAndVerifyClickToken() {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        String userId = "USER123";
        Instant expiresAt = Instant.now().plusSeconds(300);

        // When
        String token = tokenService.generateClickToken(deliveryId, campaignId, userId, expiresAt);

        // Then
        assertNotNull(token);
        assertTrue(tokenService.verifyClickToken(token));
    }

    @Test
    void shouldDecodeToken() {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        String userId = "USER123";
        Instant expiresAt = Instant.now().plusSeconds(300);

        String token = tokenService.generateImpressionToken(deliveryId, campaignId, userId, expiresAt);

        // When
        TrackingTokenData decoded = tokenService.decodeImpressionToken(token);

        // Then
        assertEquals(deliveryId, decoded.deliveryId());
        assertEquals(campaignId, decoded.campaignId());
        assertEquals(userId, decoded.userId());
    }

    @Test
    void shouldHandleAnonymousUser() {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        Instant expiresAt = Instant.now().plusSeconds(300);

        // When
        String token = tokenService.generateImpressionToken(deliveryId, campaignId, null, expiresAt);

        // Then
        assertTrue(tokenService.verifyImpressionToken(token));
        
        TrackingTokenData decoded = tokenService.decodeImpressionToken(token);
        assertNull(decoded.userId());
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        String userId = "USER123";
        Instant expiresAt = Instant.now().plusSeconds(1);

        String token = tokenService.generateImpressionToken(deliveryId, campaignId, userId, expiresAt);

        // Wait for expiration
        Thread.sleep(1500);

        // When/Then
        assertFalse(tokenService.verifyImpressionToken(token));
    }

    @Test
    void shouldRejectTamperedToken() {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        String userId = "USER123";
        Instant expiresAt = Instant.now().plusSeconds(300);

        String token = tokenService.generateImpressionToken(deliveryId, campaignId, userId, expiresAt);
        
        // Tamper with token
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // When/Then
        assertFalse(tokenService.verifyImpressionToken(tamperedToken));
    }

    @Test
    void shouldRejectWrongTokenType() {
        // Given
        String deliveryId = "DEL123";
        String campaignId = "CAMP123";
        String userId = "USER123";
        Instant expiresAt = Instant.now().plusSeconds(300);

        String impressionToken = tokenService.generateImpressionToken(deliveryId, campaignId, userId, expiresAt);

        // When/Then - impression token should not verify as click token
        assertFalse(tokenService.verifyClickToken(impressionToken));
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        String invalidToken = "invalid-token-format";

        // When/Then
        assertFalse(tokenService.verifyImpressionToken(invalidToken));
    }
}
