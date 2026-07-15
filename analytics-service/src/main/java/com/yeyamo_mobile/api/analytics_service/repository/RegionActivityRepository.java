package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.RegionActivity;

public interface RegionActivityRepository extends ElasticsearchRepository<RegionActivity, UUID> {
    List<RegionActivity> findByRegionIdAndActivityDateBetweenOrderByActivityDateDesc(
            String regionId, LocalDate from, LocalDate to);
}
