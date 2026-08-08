package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Cultural expert or institution reviewer with defined scope.
 * Cannot have global moderation power by default.
 */
@Entity
@Table(name = "cultural_reviewers", indexes = {
    @Index(name = "idx_reviewer_user", columnList = "user_id", unique = true),
    @Index(name = "idx_reviewer_role", columnList = "role"),
    @Index(name = "idx_reviewer_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CulturalReviewer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Size(max = 120)
    @Column(name = "user_id", nullable = false, unique = true, length = 120)
    private String userId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewerRole role;
    
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    
    @Size(max = 1000)
    @Column(length = 1000)
    private String credentials;
    
    /**
     * Scope: Country codes this reviewer can review (e.g., ["CM", "SN"])
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reviewer_country_scope", 
                     joinColumns = @JoinColumn(name = "reviewer_id"))
    @Column(name = "country_code", length = 2)
    @Builder.Default
    private Set<String> countryCodes = new HashSet<>();
    
    /**
     * Scope: Culture IDs this reviewer can review
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reviewer_culture_scope", 
                     joinColumns = @JoinColumn(name = "reviewer_id"))
    @Column(name = "culture_id", length = 120)
    @Builder.Default
    private Set<String> cultureIds = new HashSet<>();
    
    /**
     * Scope: Language codes this reviewer can review
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reviewer_language_scope", 
                     joinColumns = @JoinColumn(name = "reviewer_id"))
    @Column(name = "language_code", length = 10)
    @Builder.Default
    private Set<String> languageCodes = new HashSet<>();
    
    /**
     * Scope: Content types this reviewer can review
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reviewer_content_scope", 
                     joinColumns = @JoinColumn(name = "reviewer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 40)
    @Builder.Default
    private Set<TargetType> contentTypes = new HashSet<>();
    
    @NotNull
    @Size(min = 3, max = 20)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";  // ACTIVE, SUSPENDED, REVOKED
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Check if reviewer can review content from a specific country
     */
    public boolean canReviewCountry(String countryCode) {
        return role == ReviewerRole.ADMIN || countryCodes.isEmpty() || countryCodes.contains(countryCode);
    }
    
    /**
     * Check if reviewer can review a specific culture
     */
    public boolean canReviewCulture(String cultureId) {
        return role == ReviewerRole.ADMIN || cultureIds.isEmpty() || cultureIds.contains(cultureId);
    }
    
    /**
     * Check if reviewer can review a specific content type
     */
    public boolean canReviewContentType(TargetType contentType) {
        return role == ReviewerRole.ADMIN || contentTypes.isEmpty() || contentTypes.contains(contentType);
    }
    
    /**
     * Check if reviewer is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
