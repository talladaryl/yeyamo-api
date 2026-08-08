package com.yeyamo_mobile.api.analytics_service.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Daily artisan KPIs. Maps to {@code artisan_kpis_daily}. */
@Entity @Table(name = "artisan_kpis_daily")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArtisanKpisDaily {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aggregation_date", nullable = false) private LocalDate aggregationDate;
    @Column(name = "artisan_id",       nullable = false, length = 120) private String artisanId;

    @Builder.Default @Column(name = "new_followers")    private Long newFollowers   = 0L;
    @Builder.Default @Column(name = "total_followers")  private Long totalFollowers = 0L;
    @Builder.Default @Column(name = "profile_views")    private Long profileViews   = 0L;
    @Column(name = "active_artworks")  private Integer activeArtworks;
    @Column(name = "new_artworks")     private Integer newArtworks;
    @Builder.Default @Column(name = "total_views")         private Long totalViews        = 0L;
    @Column(name = "avg_views_per_artwork", precision = 10, scale = 2) private BigDecimal avgViewsPerArtwork;
    @Builder.Default @Column(name = "total_likes")  private Long totalLikes  = 0L;
    @Builder.Default @Column(name = "total_shares") private Long totalShares = 0L;
    @Builder.Default @Column(name = "total_saves")  private Long totalSaves  = 0L;
    @Column(name = "engagement_rate", precision = 5, scale = 2) private BigDecimal engagementRate;
    @Builder.Default @Column                                    private Long inquiries  = 0L;
    @Builder.Default @Column                                    private Long sales      = 0L;
    @Column(precision = 12, scale = 2) private BigDecimal revenue;
    @Column(name = "avg_sale_value",          precision = 12, scale = 2) private BigDecimal avgSaleValue;
    @Column(name = "reach_countries")         private Integer reachCountries;
    @Column(name = "international_revenue_pct", precision = 5, scale = 2) private BigDecimal internationalRevenuePct;
    @Column(name = "verification_status",     length = 30) private String verificationStatus;
    @Column(name = "authenticity_claims")     private Integer authenticityClaims;
    @Column(name = "verified_artworks")       private Integer verifiedArtworks;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
