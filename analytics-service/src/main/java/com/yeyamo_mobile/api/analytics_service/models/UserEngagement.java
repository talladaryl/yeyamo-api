package com.yeyamo_mobile.api.analytics_service.models;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "user_engagement")
@Getter
@Setter
public class UserEngagement {

    @Id
    private UUID id = UUID.randomUUID();

    private String userId;
    private LocalDate engagementDate;
    private Integer postsCount = 0;
    private Integer likesGiven = 0;
    private Integer likesReceived = 0;
    private Integer commentsCount = 0;
    private Integer sharesCount = 0;
    private Integer checkInsCount = 0;
    private Integer bookingsCount = 0;
    private Set<UUID> appliedEventIds = new LinkedHashSet<>();
    @Version
    private Long version;

    public boolean apply(UUID eventId, int posts, int likesGiven, int likesReceived, int comments,
            int shares, int checkIns, int bookings) {
        if (!appliedEventIds.add(eventId)) {
            return false;
        }
        postsCount = nonNegative(postsCount + posts);
        this.likesGiven = nonNegative(this.likesGiven + likesGiven);
        this.likesReceived = nonNegative(this.likesReceived + likesReceived);
        commentsCount = nonNegative(commentsCount + comments);
        sharesCount = nonNegative(sharesCount + shares);
        checkInsCount = nonNegative(checkInsCount + checkIns);
        bookingsCount = nonNegative(bookingsCount + bookings);
        return true;
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }
}
