package com.yeyamo_mobile.api.analytics_service.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Daily artwork popularity and commerce metrics. Maps to {@code artwork_popularity_daily}. */
@Entity @Table(name = "artwork_popularity_daily")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArtworkPopularityDaily {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aggregation_date", nullable = false) private LocalDate aggregationDate;
    @Column(name = "country_code", length = 2) private String countryCode;
    @Column(name = "artwork_id",       nullable = false, length = 120) private String artworkId;
    @Column(name = "artisan_id",       nullable = false, length = 120) private String artisanId;

    @Builder.Default @Column private Long views          = 0L;
    @Builder.Default @Column(name = "unique_viewers") private Long uniqueViewers = 0L;
    @Builder.Default @Column private Long likes          = 0L;
    @Builder.Default @Column private Long shares         = 0L;
    @Builder.Default @Column private Long saves          = 0L;
    @Builder.Default @Column(name = "story_views")    private Long storyViews    = 0L;
    @Builder.Default @Column(name = "story_completed")private Long storyCompleted= 0L;
    @Builder.Default @Column private Long inquiries      = 0L;
    @Builder.Default @Column private Long purchases      = 0L;
    @Column(precision = 12, scale = 2) private BigDecimal revenue;
    @Column(name = "reach_countries") private Integer reachCountries;
    @Builder.Default @Column(name = "international_views") private Long internationalViews = 0L;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
