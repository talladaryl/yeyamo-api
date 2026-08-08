package com.yeyamo_mobile.api.ticket_service.infrastructure.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages asymmetric key pairs for signing and validating QR tokens.
 * Supports key rotation for security.
 */
@Slf4j
@Component
public class QrSigningKeyManager {
    
    private final Map<String, KeyPairInfo> keyPairs = new ConcurrentHashMap<>();
    private volatile String currentKeyId;
    private final int keyRotationDays = 90;
    
    @PostConstruct
    public void init() {
        // Initialize with first key pair
        rotateKeys();
        log.info("QR Signing Key Manager initialized with key ID: {}", currentKeyId);
    }
    
    /**
     * Get current private key for signing new tokens
     */
    public PrivateKey getCurrentPrivateKey() {
        KeyPairInfo info = keyPairs.get(currentKeyId);
        if (info == null) {
            throw new IllegalStateException("No current signing key available");
        }
        return info.keyPair().getPrivate();
    }
    
    /**
     * Get current key ID
     */
    public String getCurrentKeyId() {
        return currentKeyId;
    }
    
    /**
     * Get public key by key ID for validation
     */
    public PublicKey getPublicKey(String keyId) {
        KeyPairInfo info = keyPairs.get(keyId);
        if (info == null) {
            throw new IllegalArgumentException("Unknown key ID: " + keyId);
        }
        return info.keyPair().getPublic();
    }
    
    /**
     * Rotate signing keys (should be called periodically)
     */
    public synchronized void rotateKeys() {
        String newKeyId = "key-" + Instant.now().getEpochSecond();
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(keyRotationDays, ChronoUnit.DAYS);
        
        KeyPairInfo info = new KeyPairInfo(keyPair, createdAt, expiresAt);
        keyPairs.put(newKeyId, info);
        currentKeyId = newKeyId;
        
        log.info("Rotated signing keys. New key ID: {}, Expires: {}", newKeyId, expiresAt);
        
        // Clean up very old keys (keep keys for 2x rotation period)
        cleanupOldKeys();
    }
    
    /**
     * Check if current key should be rotated
     */
    public boolean shouldRotateKey() {
        KeyPairInfo info = keyPairs.get(currentKeyId);
        if (info == null) {
            return true;
        }
        // Rotate if key is more than 80% through its lifetime
        long lifetimeDays = ChronoUnit.DAYS.between(info.createdAt(), info.expiresAt());
        long ageInDays = ChronoUnit.DAYS.between(info.createdAt(), Instant.now());
        return ageInDays >= (lifetimeDays * 0.8);
    }
    
    /**
     * Check if a key ID is valid (exists and not expired)
     */
    public boolean isKeyIdValid(String keyId) {
        KeyPairInfo info = keyPairs.get(keyId);
        return info != null && Instant.now().isBefore(info.expiresAt());
    }
    
    private void cleanupOldKeys() {
        Instant cutoff = Instant.now().minus(keyRotationDays * 2L, ChronoUnit.DAYS);
        keyPairs.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(cutoff));
    }
    
    /**
     * Record class to hold key pair with metadata
     */
    private record KeyPairInfo(KeyPair keyPair, Instant createdAt, Instant expiresAt) {}
}
