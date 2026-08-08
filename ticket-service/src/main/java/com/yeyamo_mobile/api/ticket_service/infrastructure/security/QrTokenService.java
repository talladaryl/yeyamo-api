package com.yeyamo_mobile.api.ticket_service.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for creating and validating cryptographically signed QR tokens.
 * Uses JWT with RSA signatures for tamper-proof tickets.
 * 
 * Security principles:
 * - Asymmetric signatures (RS256)
 * - Minimal claims (no PII)
 * - Token ID for revocation
 * - Key rotation support
 * - Hash storage (not full token)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrTokenService {
    
    private final QrSigningKeyManager keyManager;
    
    /**
     * Generate a signed QR token for a ticket
     * 
     * @param ticketId Ticket identifier
     * @param eventId Event identifier
     * @param expiryDays Token expiry in days
     * @return QrTokenData containing token and metadata
     */
    public QrTokenData generateToken(String ticketId, String eventId, int expiryDays) {
        String tokenId = UUID.randomUUID().toString();
        String keyId = keyManager.getCurrentKeyId();
        Instant now = Instant.now();
        Instant expiry = now.plus(expiryDays, ChronoUnit.DAYS);
        
        String token = Jwts.builder()
                .setId(tokenId)
                .setSubject(ticketId)
                .claim("eventId", eventId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .setHeaderParam("kid", keyId)
                .signWith(keyManager.getCurrentPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
        
        String tokenHash = hashToken(token);
        
        log.debug("Generated QR token for ticket: {}, tokenId: {}, keyId: {}", 
                  ticketId, tokenId, keyId);
        
        return new QrTokenData(token, tokenId, tokenHash, keyId, now, expiry);
    }
    
    /**
     * Validate and parse a QR token
     * 
     * @param token JWT token string
     * @return TokenValidationResult with validation status and claims
     */
    public TokenValidationResult validateToken(String token) {
        try {
            // First parse without validation to get key ID
            Jwt<?, ?> unverifiedToken = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(token.substring(0, token.lastIndexOf('.') + 1));
            
            String keyId = (String) unverifiedToken.getHeader().get("kid");
            if (keyId == null || !keyManager.isKeyIdValid(keyId)) {
                log.warn("Invalid or unknown key ID in token");
                return TokenValidationResult.invalid("Invalid signing key");
            }
            
            // Now validate with proper public key
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(keyManager.getPublicKey(keyId))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String tokenId = claims.getId();
            String ticketId = claims.getSubject();
            String eventId = claims.get("eventId", String.class);
            Instant issuedAt = claims.getIssuedAt().toInstant();
            Instant expiresAt = claims.getExpiration().toInstant();
            
            log.debug("Validated QR token - ticketId: {}, tokenId: {}", ticketId, tokenId);
            
            return TokenValidationResult.valid(tokenId, ticketId, eventId, keyId, issuedAt, expiresAt);
            
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return TokenValidationResult.invalid("Token expired");
        } catch (SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return TokenValidationResult.invalid("Invalid signature");
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return TokenValidationResult.invalid("Malformed token");
        } catch (Exception e) {
            log.error("Token validation error", e);
            return TokenValidationResult.invalid("Validation failed");
        }
    }
    
    /**
     * Calculate SHA-256 hash of token for storage
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Data class for generated QR token
     */
    public record QrTokenData(
            String token,
            String tokenId,
            String tokenHash,
            String keyId,
            Instant issuedAt,
            Instant expiresAt
    ) {}
    
    /**
     * Result of token validation
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String tokenId;
        private final String ticketId;
        private final String eventId;
        private final String keyId;
        private final Instant issuedAt;
        private final Instant expiresAt;
        
        private TokenValidationResult(boolean valid, String errorMessage, 
                                      String tokenId, String ticketId, String eventId, 
                                      String keyId, Instant issuedAt, Instant expiresAt) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.tokenId = tokenId;
            this.ticketId = ticketId;
            this.eventId = eventId;
            this.keyId = keyId;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
        
        public static TokenValidationResult valid(String tokenId, String ticketId, 
                                                  String eventId, String keyId, 
                                                  Instant issuedAt, Instant expiresAt) {
            return new TokenValidationResult(true, null, tokenId, ticketId, eventId, 
                                            keyId, issuedAt, expiresAt);
        }
        
        public static TokenValidationResult invalid(String errorMessage) {
            return new TokenValidationResult(false, errorMessage, null, null, null, 
                                            null, null, null);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getTokenId() { return tokenId; }
        public String getTicketId() { return ticketId; }
        public String getEventId() { return eventId; }
        public String getKeyId() { return keyId; }
        public Instant getIssuedAt() { return issuedAt; }
        public Instant getExpiresAt() { return expiresAt; }
    }
}
