package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Claim that an artwork or cultural content is authentic.
 * Requires evidence and expert review.
 */
@Entity
@Table(name = "authenticity_claims", indexes = {
    @Index(name = "idx_auth_claim_target", columnList = "target_type,target_id"),
    @Index(name = "idx_auth_claim_claimant", columnList = "claimant_id"),
    @Index(name = "idx_auth_claim_status", columnList = "status"),
    @Index(name = "idx_auth_claim_reviewer", columnList = "assigned_reviewer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticityClaim {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private TargetType targetType;
    
    @NotNull
    @Size(max = 120)
    @Column(name = "target_id", nullable = false, length = 120)
    private String targetId;
    
    @NotNull
    @Size(max = 120)
    @Column(name = "claimant_id", nullable = false, length = 120)
    private String claimantId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AuthenticityStatus status = AuthenticityStatus.DECLARED;
    
    @Lob
    @Column(name = "claim_description", columnDefinition = "TEXT")
    private String claimDescription;
    
    @Size(max = 120)
    @Column(name = "assigned_reviewer_id", length = 120)
    private String assignedReviewerId;
    
    @Lob
    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;
    
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    @Column(name = "rejected_at")
    private Instant rejectedAt;
    
    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(nullable = false)
    private Long version;
    
    /**
     * Verify the claim
     */
    public void verify(String reviewerId) {
        if (this.status != AuthenticityStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only verify claims under review");
        }
        this.status = AuthenticityStatus.VERIFIED;
        this.assignedReviewerId = reviewerId;
        this.verifiedAt = Instant.now();
    }
    
    /**
     * Reject the claim
     */
    public void reject(String reviewerId, String reason) {
        if (this.status != AuthenticityStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only reject claims under review");
        }
        this.status = AuthenticityStatus.REJECTED;
        this.assignedReviewerId = reviewerId;
        this.rejectedAt = Instant.now();
        this.rejectionReason = reason;
    }
    
    /**
     * Revoke a previously verified claim
     */
    public void revoke(String reason) {
        if (this.status != AuthenticityStatus.VERIFIED) {
            throw new IllegalStateException("Can only revoke verified claims");
        }
        this.status = AuthenticityStatus.REVOKED;
        this.rejectionReason = reason;
    }
}
