package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;

@Entity
@Table(name = "recommendation_candidates")
public class CandidateEntity {
    @Id @Column(name = "source_id", length = 160) String sourceId;
    @Column(name = "target_id", nullable = false, length = 120) String targetId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) CandidateKind kind;
    @Column(nullable = false, length = 300) String title;
    @Column(name = "category_code", length = 100) String categoryCode;
    @Column(name = "region_code", length = 100) String regionCode;
    @Column(name = "country_code", length = 2) String countryCode;
    @Column(name = "language_code", length = 10) String languageCode;
    Double latitude;
    Double longitude;
    @Column(nullable = false) double popularity;
    @Column(nullable = false) boolean active;
    @Column(name = "published_at") Instant publishedAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(precision = 19, scale = 2) BigDecimal price;
    @Column(name = "currency_code", length = 3) String currencyCode;
    @Column(name = "image_media_id") UUID imageMediaId;
    @Column(name = "location_label", length = 300) String locationLabel;
    @Column(name = "starts_at") Instant startsAt;
    @Column(name = "ends_at") Instant endsAt;
    @Version long version;
}
