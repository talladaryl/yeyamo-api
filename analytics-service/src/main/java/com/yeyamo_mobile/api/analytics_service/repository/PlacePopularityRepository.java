package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;

public interface PlacePopularityRepository extends ElasticsearchRepository<PlacePopularity, UUID> {
    List<PlacePopularity> findTop20ByOrderByPopularityScoreDesc();
    List<PlacePopularity> findByPlaceId(UUID placeId);
}
