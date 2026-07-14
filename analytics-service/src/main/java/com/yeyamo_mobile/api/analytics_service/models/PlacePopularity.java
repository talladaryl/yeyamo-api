package com.yeyamo_mobile.api.analytics_service.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
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
    private BigDecimal popularityScore = BigDecimal.ZERO;
    private Integer views30d = 0;
    private Integer likes30d = 0;
    private LocalDateTime updatedAt = LocalDateTime.now();
}
