package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Rate limiter pour les souscriptions WebSocket.
 * 
 * Protection contre:
 * - Énumération d'ID de conversation (tentatives répétées)
 * - Abus de ressources serveur
 * 
 * Utilise Guava RateLimiter pour limiter les tentatives par utilisateur.
 */
@Component
public class WebSocketRateLimiter {
    
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final double permitsPerMinute;
    
    public WebSocketRateLimiter(
            @Value("${websocket.rate-limit.subscribe-per-minute:10}") double permitsPerMinute) {
        this.permitsPerMinute = permitsPerMinute;
    }
    
    /**
     * Vérifie si une tentative de souscription est autorisée selon le rate limit.
     * 
     * @param userId ID de l'utilisateur
     * @return true si autorisé, false si limite dépassée
     */
    public boolean tryAcquire(String userId) {
        RateLimiter limiter = limiters.computeIfAbsent(userId, 
            k -> RateLimiter.create(permitsPerMinute / 60.0)); // Convertir en permits/seconde
        
        // Tenter d'acquérir un permit avec timeout de 0 (non-bloquant)
        return limiter.tryAcquire(0, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Nettoie les rate limiters inactifs (maintenance périodique).
     * 
     * Note: Pour production, considérer une solution avec expiration automatique
     * (ex: Caffeine cache avec eviction) si le nombre d'utilisateurs est élevé.
     */
    public void cleanupInactive() {
        // Pour simplification, on garde tous les limiters
        // En production: utiliser un cache avec TTL (ex: Caffeine)
        
        // Optionnel: Nettoyer si taille > seuil
        if (limiters.size() > 10000) {
            limiters.clear(); // Reset brutal (acceptable pour rate limiting)
        }
    }
}
