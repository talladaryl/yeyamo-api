package com.yeyamo_mobile.api.analytics_service.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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
    private BigDecimal grossBookingValue = BigDecimal.ZERO;
    private Long totalCheckIns = 0L;
    private Set<UUID> appliedEventIds = new LinkedHashSet<>();
    @Version
    private Long version;

    public boolean apply(UUID eventId, long views, long bookings, long reviews,
            BigDecimal rating, BigDecimal bookingValue, long checkIns) {
        if (!appliedEventIds.add(eventId)) {
            return false;
        }
        totalViews = Math.max(0, totalViews + views);
        totalBookings = Math.max(0, totalBookings + bookings);
        if (reviews > 0) {
            BigDecimal totalRating = avgRating.multiply(BigDecimal.valueOf(totalReviews));
            totalReviews += reviews;
            avgRating = totalRating.add(rating.multiply(BigDecimal.valueOf(reviews)))
                    .divide(BigDecimal.valueOf(totalReviews), 2, java.math.RoundingMode.HALF_UP);
        }
        grossBookingValue = grossBookingValue.add(bookingValue);
        if (grossBookingValue.signum() < 0) {
            grossBookingValue = BigDecimal.ZERO;
        }
        totalCheckIns = Math.max(0, totalCheckIns + checkIns);
        return true;
    }
}
