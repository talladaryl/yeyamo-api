package com.yeyamo_mobile.api.analytics_service.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "place_popularity")
@Getter
@Setter
public class PlacePopularity {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID placeId;
    private LocalDate statDate;
    private BigDecimal popularityScore = BigDecimal.ZERO;
    private Integer views = 0;
    private Integer likes = 0;
    private Integer comments = 0;
    private Integer shares = 0;
    private Integer checkIns = 0;
    private Integer bookings = 0;
    private Integer posts = 0;
    private Instant updatedAt = Instant.now();
    private Set<UUID> appliedEventIds = new LinkedHashSet<>();
    @Version
    private Long version;

    public boolean apply(UUID eventId, int views, int likes, int comments, int shares,
            int checkIns, int bookings, int posts, Instant occurredAt) {
        if (!appliedEventIds.add(eventId)) {
            return false;
        }
        this.views = nonNegative(this.views + views);
        this.likes = nonNegative(this.likes + likes);
        this.comments = nonNegative(this.comments + comments);
        this.shares = nonNegative(this.shares + shares);
        this.checkIns = nonNegative(this.checkIns + checkIns);
        this.bookings = nonNegative(this.bookings + bookings);
        this.posts = nonNegative(this.posts + posts);
        popularityScore = BigDecimal.valueOf(this.views)
                .add(BigDecimal.valueOf(this.likes * 3L))
                .add(BigDecimal.valueOf(this.comments * 2L))
                .add(BigDecimal.valueOf(this.shares * 4L))
                .add(BigDecimal.valueOf(this.checkIns * 6L))
                .add(BigDecimal.valueOf(this.bookings * 8L))
                .add(BigDecimal.valueOf(this.posts * 2L));
        updatedAt = occurredAt;
        return true;
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }
}
