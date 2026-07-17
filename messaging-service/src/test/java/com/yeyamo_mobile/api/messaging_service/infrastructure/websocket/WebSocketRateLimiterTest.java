package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebSocketRateLimiterTest {

    private WebSocketRateLimiter rateLimiter;
    private String userId;

    @BeforeEach
    void setUp() {
        // Configurer avec 10 permits par minute = 0.166 permits/seconde
        rateLimiter = new WebSocketRateLimiter(10.0);
        userId = "user-123";
    }

    // ─── TESTS RATE LIMITING ─────────────────────────────────────────────────────

    @Test
    void shouldAllowFirstAttempt() {
        boolean allowed = rateLimiter.tryAcquire(userId);

        assertTrue(allowed, "First attempt should be allowed");
    }

    @Test
    void shouldAllowMultipleAttemptsWithinLimit() {
        // Les 10 premiers permits devraient être disponibles immédiatement
        for (int i = 0; i < 5; i++) {
            boolean allowed = rateLimiter.tryAcquire(userId);
            assertTrue(allowed, "Attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void shouldRejectWhenLimitExceeded() {
        // Consommer tous les permits disponibles
        // Note: Avec warmup period de RateLimiter, les premiers permits sont disponibles
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(userId);
        }

        // La prochaine tentative devrait être rejetée
        boolean allowed = rateLimiter.tryAcquire(userId);

        assertFalse(allowed, "Should reject when limit exceeded");
    }

    @Test
    void shouldIsolateLimitsBetweenUsers() {
        String user1 = "user-1";
        String user2 = "user-2";

        // Consommer tous les permits de user1
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(user1);
        }
        boolean user1Blocked = rateLimiter.tryAcquire(user1);

        // User2 devrait toujours avoir ses permits
        boolean user2Allowed = rateLimiter.tryAcquire(user2);

        assertFalse(user1Blocked, "User1 should be blocked");
        assertTrue(user2Allowed, "User2 should still be allowed");
    }

    @Test
    void shouldAllowAfterTimeWindow() throws InterruptedException {
        // Consommer tous les permits
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(userId);
        }
        assertFalse(rateLimiter.tryAcquire(userId), "Should be blocked initially");

        // Attendre suffisamment pour qu'un nouveau permit soit disponible
        // 10 permits/minute = 1 permit tous les 6 secondes
        // Attendre 1 seconde devrait donner ~0.166 permits (arrondi à 0)
        // Attendre 7 secondes devrait donner au moins 1 permit
        Thread.sleep(7000);

        boolean allowed = rateLimiter.tryAcquire(userId);

        assertTrue(allowed, "Should allow after time window");
    }

    // ─── TESTS MAINTENANCE ───────────────────────────────────────────────────────

    @Test
    void shouldHandleCleanup() {
        // Créer beaucoup d'entrées
        for (int i = 0; i < 100; i++) {
            rateLimiter.tryAcquire("user-" + i);
        }

        // Le cleanup ne devrait pas planter
        assertDoesNotThrow(() -> rateLimiter.cleanupInactive());
    }
}
