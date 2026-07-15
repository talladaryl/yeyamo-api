package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;

public interface PlacePopularityRepository extends ElasticsearchRepository<PlacePopularity, UUID> {
    List<PlacePopularity> findByStatDateBetween(LocalDate from, LocalDate to);
    List<PlacePopularity> findByPlaceIdAndStatDateBetweenOrderByStatDateDesc(
            UUID placeId, LocalDate from, LocalDate to);
}
