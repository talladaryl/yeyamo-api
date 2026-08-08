package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Copyright infringement claim.
 * Follows DMCA-like workflow with notice, response, and resolution.
 */
@Entity
@Table(name = "copyright_claims", indexes = {
    @Index(name = "idx_copyright_target", columnList = "target_type,target_id"),
    @Index(name = "idx_copyright_claimant", columnList = "claimant_id"),
    @Index(name = "idx_copyright_status", columnList = "status"),
    @Index(name = "idx_copyright_creator", columnList = "content_creator_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyrightClaim {
    
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
    
    @Size(max = 120)
    @Column(name = "content_creator_id", length = 120)
    private String contentCreatorId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CopyrightClaimStatus status = CopyrightClaimStatus.FILED;
    
    @NotNull
    @Lob
    @Column(name = "claim_details", nullable = false, columnDefinition = "TEXT")
    private String claimDetails;
    
    @Size(max = 500)
    @Column(name = "proof_url", length = 500)
    private String proofUrl;
    
    @Lob
    @Column(name = "creator_response", columnDefinition = "TEXT")
    private String creatorResponse;
    
    @Column(name = "creator_response_at")
    private Instant creatorResponseAt;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    
    @Size(max = 120)
    @Column(name = "resolved_by", length = 120)
    private String resolvedBy;
    
    @Column(name = "content_removed")
    @Builder.Default
    private Boolean contentRemoved = false;
    
    @Column(name = "content_restored")
    @Builder.Default
    private Boolean contentRestored = false;
    
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
     * Remove content pending review
     */
    public void removeContent() {
        this.status = CopyrightClaimStatus.CONTENT_REMOVED;
        this.contentRemoved = true;
    }
    
    /**
     * Record creator's response
     */
    public void recordCreatorResponse(String response) {
        this.creatorResponse = response;
        this.creatorResponseAt = Instant.now();
        this.status = CopyrightClaimStatus.UNDER_REVIEW;
    }
    
    /**
     * Uphold claim - keep content removed
     */
    public void uphold(String reviewerId, String resolution) {
        this.status = CopyrightClaimStatus.UPHELD;
        this.resolvedBy = reviewerId;
        this.resolution = resolution;
        this.resolvedAt = Instant.now();
    }
    
    /**
     * Reject claim - restore content
     */
    public void reject(String reviewerId, String resolution) {
        this.status = CopyrightClaimStatus.REJECTED;
        this.resolvedBy = reviewerId;
        this.resolution = resolution;
        this.resolvedAt = Instant.now();
        this.contentRestored = true;
    }
}
