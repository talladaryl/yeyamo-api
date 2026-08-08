package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Evidence submitted to support an authenticity claim.
 * Can be documents, photos, certifications, etc.
 */
@Entity
@Table(name = "authenticity_evidence", indexes = {
    @Index(name = "idx_auth_evidence_claim", columnList = "claim_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticityEvidence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Size(max = 120)
    @Column(name = "claim_id", nullable = false, length = 120)
    private String claimId;
    
    @NotNull
    @Size(min = 3, max = 50)
    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType;  // CERTIFICATE, PHOTO, DOCUMENT, VIDEO, etc.
    
    @NotNull
    @Size(max = 500)
    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;
    
    @Size(max = 1000)
    @Column(length = 1000)
    private String description;
    
    @Size(max = 120)
    @Column(name = "uploaded_by", nullable = false, length = 120)
    private String uploadedBy;
    
    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
}
