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

@Document(indexName = "region_activity")
@Getter
@Setter
public class RegionActivity {

    @Id
    private UUID id = UUID.randomUUID();

    private String regionId;
    private LocalDate activityDate;
    private Integer totalUsers = 0;
    private Integer activeUsers = 0;
    private Integer totalPlaces = 0;
    private Integer totalPosts = 0;
    private Integer totalCheckIns = 0;
    private Set<String> activeUserIds = new LinkedHashSet<>();
    private Set<UUID> appliedEventIds = new LinkedHashSet<>();
    @Version
    private Long version;

    public boolean apply(UUID eventId, int users, int places, int posts, int checkIns, String activeUserId) {
        if (!appliedEventIds.add(eventId)) {
            return false;
        }
        totalUsers = Math.max(0, totalUsers + users);
        totalPlaces = Math.max(0, totalPlaces + places);
        totalPosts = Math.max(0, totalPosts + posts);
        totalCheckIns = Math.max(0, totalCheckIns + checkIns);
        if (activeUserId != null && !activeUserId.isBlank()) {
            activeUserIds.add(activeUserId);
        }
        activeUsers = activeUserIds.size();
        return true;
    }
}
