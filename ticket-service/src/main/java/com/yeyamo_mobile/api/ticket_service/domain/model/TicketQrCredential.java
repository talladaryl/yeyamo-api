package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Secure QR credential for a ticket.
 * Stores cryptographically signed token metadata - NOT the token itself.
 * Token uses asymmetric signatures for tamper-proof validation.
 */
@Entity
@Table(name = "ticket_qr_credentials", indexes = {
    @Index(name = "idx_qr_ticket", columnList = "ticket_id", unique = true),
    @Index(name = "idx_qr_token_id", columnList = "token_id", unique = true),
    @Index(name = "idx_qr_key", columnList = "key_id"),
    @Index(name = "idx_qr_revoked", columnList = "revoked_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketQrCredential {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Column(name = "ticket_id", nullable = false, unique = true)
    private String ticketId;
    
    /**
     * Unique token identifier (jti claim in JWT)
     */
    @NotNull
    @Size(min = 20, max = 100)
    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;
    
    /**
     * SHA-256 hash of the token for validation and revocation
     * We store hash, not the full token
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    
    /**
     * Key ID used to sign this token (for key rotation)
     */
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "key_id", nullable = false, length = 50)
    private String keyId;
    
    @NotNull
    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;
    
    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    /**
     * If set, this credential has been revoked (security incident, refund, etc.)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    /**
     * Check if credential is revoked
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }
    
    /**
     * Check if credential has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Revoke this credential
     */
    public void revoke() {
        if (!isRevoked()) {
            this.revokedAt = Instant.now();
        }
    }
}
