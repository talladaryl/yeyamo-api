package com.yeyamo_mobile.api.ticket_service.infrastructure.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QrTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(QrTokenService.class);
    
    private final SpringTicketQrCredentialRepository credentialRepository;
    private final Map<String, KeyPair> keyStore = new ConcurrentHashMap<>();
    private volatile String currentKeyId;
    private final int tokenTtlDays;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public QrTokenService(
            SpringTicketQrCredentialRepository credentialRepository,
            @Value("${yeyamo.tickets.qr.token-ttl-days:30}") int tokenTtlDays) {
        this(credentialRepository, tokenTtlDays, "", "", "");
    }

    @Autowired
    public QrTokenService(
            SpringTicketQrCredentialRepository credentialRepository,
            @Value("${yeyamo.tickets.qr.token-ttl-days:30}") int tokenTtlDays,
            @Value("${yeyamo.tickets.qr.key-id:}") String configuredKeyId,
            @Value("${yeyamo.tickets.qr.private-key-base64:}") String privateKeyBase64,
            @Value("${yeyamo.tickets.qr.public-key-base64:}") String publicKeyBase64) {
        this.credentialRepository = credentialRepository;
        this.tokenTtlDays = tokenTtlDays;
        initializeKeys(configuredKeyId, privateKeyBase64, publicKeyBase64);
    }
    
    private void initializeKeys(String configuredKeyId, String privateKeyBase64, String publicKeyBase64) {
        try {
            if (!privateKeyBase64.isBlank() && !publicKeyBase64.isBlank()) {
                KeyFactory factory = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = factory.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64))
                );
                PublicKey publicKey = factory.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))
                );
                String keyId = configuredKeyId.isBlank() ? "configured-key" : configuredKeyId;
                keyStore.put(keyId, new KeyPair(publicKey, privateKey));
                currentKeyId = keyId;
                logger.info("Loaded persistent QR signing key: {}", keyId);
                return;
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            
            String keyId = "key-" + Instant.now().toString().substring(0, 10);
            KeyPair keyPair = generator.generateKeyPair();
            
            keyStore.put(keyId, keyPair);
            currentKeyId = keyId;
            
            logger.info("Initialized QR token signing key: {}", keyId);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to initialize RSA key pair", e);
        }
    }
    
    public String generateQrToken(TicketEntity ticket) {
        String tokenId = UUID.randomUUID().toString();
        KeyPair keyPair = keyStore.get(currentKeyId);
        
        if (keyPair == null) {
            throw new IllegalStateException("No active signing key available");
        }
        
        Instant now = Instant.now();
        Instant expiration = now.plus(tokenTtlDays, ChronoUnit.DAYS);
        
        // Build JWT with minimal claims (no PII)
        String token = Jwts.builder()
            .header()
                .add("kid", currentKeyId)
                .add("typ", "JWT")
                .and()
            .id(tokenId)
            .subject(ticket.getId().toString())
            .issuer("ticket-service")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("evt", ticket.getEventId())
            .claim("typ", "TICKET_QR")
            .claim("ver", 1)
            .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
            .compact();
        
        // Store credential (hash only for security)
        TicketQrCredentialEntity credential = credentialRepository.findByTicketId(ticket.getId())
            .orElseGet(TicketQrCredentialEntity::new);
        credential.setTicketId(ticket.getId());
        credential.setTokenId(tokenId);
        credential.setTokenHash(hashToken(token));
        credential.setKeyId(currentKeyId);
        credential.setExpiresAt(expiration);
        credential.setRevokedAt(null);
        
        credentialRepository.save(credential);
        
        logger.debug("Generated QR token for ticket: {} with token ID: {}", ticket.getId(), tokenId);
        
        return token;
    }
    
    public QrValidationResult validateQrToken(String token, String expectedEventId) {
        try {
            String keyId = readKeyId(token);
            KeyPair verificationKey = keyStore.get(keyId);
            if (verificationKey == null) {
                return QrValidationResult.failure("INVALID", "Unknown signing key");
            }

            // Resolve the public key from kid so tokens remain valid after key rotation.
            JwtParser parser = Jwts.parser()
                .verifyWith(verificationKey.getPublic())
                .requireIssuer("ticket-service")
                .build();
            
            Jws<Claims> jws = parser.parseSignedClaims(token);
            Claims claims = jws.getPayload();
            
            // Extract claims
            String tokenId = claims.getId();
            String ticketIdStr = claims.getSubject();
            String tokenEventId = claims.get("evt", String.class);
            String tokenType = claims.get("typ", String.class);

            if (!"TICKET_QR".equals(tokenType) || tokenId == null || ticketIdStr == null) {
                return QrValidationResult.failure("INVALID", "Invalid token claims");
            }
            
            // Verify event match
            if (!expectedEventId.equals(tokenEventId)) {
                logger.warn("Event mismatch: expected={}, got={}", expectedEventId, tokenEventId);
                return QrValidationResult.failure("WRONG_EVENT", "Token is for different event");
            }
            
            // Check revocation in database
            var credential = credentialRepository.findByTokenId(tokenId);
            
            if (credential.isEmpty()) {
                logger.warn("Token ID not found in database: {}", tokenId);
                return QrValidationResult.failure("INVALID", "Token not found");
            }

            UUID ticketId = UUID.fromString(ticketIdStr);
            if (!credential.get().getTicketId().equals(ticketId)
                    || !credential.get().getKeyId().equals(keyId)
                    || !constantTimeEquals(credential.get().getTokenHash(), hashToken(token))) {
                return QrValidationResult.failure("INVALID", "Token credential mismatch");
            }
            
            if (credential.get().isRevoked()) {
                logger.warn("Token revoked: {}", tokenId);
                return QrValidationResult.failure("REVOKED", "Token has been revoked");
            }
            
            if (credential.get().isExpired()) {
                logger.warn("Token expired: {}", tokenId);
                return QrValidationResult.failure("EXPIRED", "Token has expired");
            }
            
            // Success
            return QrValidationResult.success(ticketId);
            
        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired: {}", e.getMessage());
            return QrValidationResult.failure("EXPIRED", "Token has expired");
        } catch (JwtException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return QrValidationResult.failure("INVALID", "Invalid token signature");
        } catch (Exception e) {
            logger.error("Unexpected error validating QR token", e);
            return QrValidationResult.failure("INVALID", "Token validation error");
        }
    }
    
    public void revokeQrToken(UUID ticketId) {
        credentialRepository.findByTicketId(ticketId).ifPresent(credential -> {
            credential.revoke();
            credentialRepository.save(credential);
            logger.info("Revoked QR token for ticket: {}", ticketId);
        });
    }
    
    private String readKeyId(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("Malformed JWT");
        }
        byte[] header = Base64.getUrlDecoder().decode(parts[0]);
        return objectMapper.readTree(header).path("kid").asText(null);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.US_ASCII),
            actual.getBytes(StandardCharsets.US_ASCII)
        );
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    // Result class
    public static class QrValidationResult {
        private final boolean valid;
        private final UUID ticketId;
        private final String errorCode;
        private final String errorMessage;
        
        private QrValidationResult(boolean valid, UUID ticketId, String errorCode, String errorMessage) {
            this.valid = valid;
            this.ticketId = ticketId;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        public static QrValidationResult success(UUID ticketId) {
            return new QrValidationResult(true, ticketId, null, null);
        }
        
        public static QrValidationResult failure(String errorCode, String errorMessage) {
            return new QrValidationResult(false, null, errorCode, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public UUID getTicketId() { return ticketId; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
    }
}
