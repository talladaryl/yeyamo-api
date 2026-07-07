package com.yeyamo_mobile.api.analytics_service.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "partner_analytics")
@Getter
@Setter
public class PartnerAnalytics {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID partnerId;
    private LocalDate statDate;
    private Long totalViews = 0L;
    private Long totalBookings = 0L;
    private Long totalReviews = 0L;
    private BigDecimal avgRating = BigDecimal.ZERO;
}
