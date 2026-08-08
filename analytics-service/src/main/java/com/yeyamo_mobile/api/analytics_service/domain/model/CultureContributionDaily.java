package com.yeyamo_mobile.api.analytics_service.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Daily contribution analytics. Maps to {@code culture_contribution_daily}. */
@Entity @Table(name = "culture_contribution_daily")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CultureContributionDaily {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aggregation_date",   nullable = false) private LocalDate aggregationDate;
    @Column(name = "country_code",       length = 2)       private String countryCode;
    @Column(name = "contribution_type",  nullable = false, length = 50) private String contributionType;

    @Builder.Default @Column(name = "total_contributions")   private Long totalContributions  = 0L;
    @Builder.Default @Column(name = "unique_contributors")   private Long uniqueContributors  = 0L;
    @Builder.Default @Column(name = "verified_contributions")private Long verifiedContributions = 0L;
    @Builder.Default @Column(name = "rejected_contributions")private Long rejectedContributions = 0L;
    @Builder.Default @Column(name = "pending_contributions") private Long pendingContributions  = 0L;
    @Column(name = "verification_rate", precision = 5, scale = 2) private BigDecimal verificationRate;
    @Builder.Default @Column(name = "views_generated")      private Long viewsGenerated      = 0L;
    @Column(name = "avg_quality_score", precision = 3, scale = 2) private BigDecimal avgQualityScore;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
