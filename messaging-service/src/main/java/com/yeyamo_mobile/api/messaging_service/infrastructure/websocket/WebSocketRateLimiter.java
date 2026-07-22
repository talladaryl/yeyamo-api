package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rate limiter pour les souscriptions WebSocket.
 * 
 * Protection contre:
 * - Énumération d'ID de conversation (tentatives répétées)
 * - Abus de ressources serveur
 * 
 * Utilise un token bucket par utilisateur. Le bucket démarre plein, puis se
 * recharge progressivement selon la limite configurée par minute.
 */
@Component
public class WebSocketRateLimiter {
    
    private static final long INACTIVITY_TTL_NANOS = TimeUnit.HOURS.toNanos(1);

    private final ConcurrentHashMap<String, TokenBucket> limiters = new ConcurrentHashMap<>();
    private final double permitsPerMinute;
    
    public WebSocketRateLimiter(
            @Value("${websocket.rate-limit.subscribe-per-minute:10}") double permitsPerMinute) {
        if (permitsPerMinute <= 0) {
            throw new IllegalArgumentException("permitsPerMinute must be greater than zero");
        }
        this.permitsPerMinute = permitsPerMinute;
    }
    
    /**
     * Vérifie si une tentative de souscription est autorisée selon le rate limit.
     * 
     * @param userId ID de l'utilisateur
     * @return true si autorisé, false si limite dépassée
     */
    public boolean tryAcquire(String userId) {
        TokenBucket limiter = limiters.computeIfAbsent(userId,
            ignored -> new TokenBucket(permitsPerMinute));
        
        return limiter.tryAcquire();
    }
    
    /**
     * Nettoie les rate limiters inactifs (maintenance périodique).
     * 
     * Les buckets qui n'ont pas été utilisés depuis une heure sont supprimés.
     */
    public void cleanupInactive() {
        long cutoff = System.nanoTime() - INACTIVITY_TTL_NANOS;
        limiters.entrySet().removeIf(entry -> entry.getValue().lastAccessNanos() < cutoff);
    }

    private static final class TokenBucket {
        private final double capacity;
        private final double permitsPerNano;
        private double availablePermits;
        private long lastRefillNanos;
        private long lastAccessNanos;

        private TokenBucket(double permitsPerMinute) {
            this.capacity = permitsPerMinute;
            this.permitsPerNano = permitsPerMinute / TimeUnit.MINUTES.toNanos(1);
            this.availablePermits = permitsPerMinute;
            this.lastRefillNanos = System.nanoTime();
            this.lastAccessNanos = lastRefillNanos;
        }

        private synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            refill(now);
            lastAccessNanos = now;
            if (availablePermits < 1.0) {
                return false;
            }
            availablePermits -= 1.0;
            return true;
        }

        private synchronized long lastAccessNanos() {
            return lastAccessNanos;
        }

        private void refill(long now) {
            long elapsedNanos = now - lastRefillNanos;
            if (elapsedNanos > 0) {
                availablePermits = Math.min(capacity,
                    availablePermits + elapsedNanos * permitsPerNano);
                lastRefillNanos = now;
            }
        }
    }
}
