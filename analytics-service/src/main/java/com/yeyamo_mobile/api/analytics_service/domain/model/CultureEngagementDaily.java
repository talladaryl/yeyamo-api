package com.yeyamo_mobile.api.analytics_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Daily aggregated culture content engagement metrics
 */
@Entity
@Table(name = "culture_engagement_daily")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CultureEngagementDaily {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Column(name = "aggregation_date", nullable = false)
    private LocalDate aggregationDate;
    
    @Size(max = 2)
    @Column(name = "country_code", length = 2)
    private String countryCode;
    
    @Size(max = 120)
    @Column(name = "culture_id", length = 120)
    private String cultureId;
    
    @Size(max = 10)
    @Column(name = "language_code", length = 10)
    private String languageCode;
    
    // Views and engagement
    @Column(name = "content_views", nullable = false)
    @Builder.Default
    private Long contentViews = 0L;
    
    @Column(name = "unique_viewers", nullable = false)
    @Builder.Default
    private Long uniqueViewers = 0L;
    
    @Column(name = "avg_view_duration_seconds")
    private Integer avgViewDurationSeconds;
    
    @Column(name = "total_view_duration_seconds", nullable = false)
    @Builder.Default
    private Long totalViewDurationSeconds = 0L;
    
    // Completions
    @Column(name = "content_completed", nullable = false)
    @Builder.Default
    private Long contentCompleted = 0L;
    
    @Column(name = "completion_rate", precision = 5, scale = 2)
    private BigDecimal completionRate;
    
    // Interactions
    @Column(nullable = false)
    @Builder.Default
    private Long likes = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long shares = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long saves = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long comments = 0L;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
