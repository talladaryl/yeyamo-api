package com.yeyamo_mobile.api.analytics_service.models;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "region_activity")
@Getter
@Setter
public class RegionActivity {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID regionId;
    private LocalDate activityDate;
    private Integer totalUsers = 0;
    private Integer activeUsers = 0;
    private Integer totalPlaces = 0;
    private Integer totalPosts = 0;
}
