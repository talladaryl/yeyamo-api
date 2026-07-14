package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.RegionActivity;

public interface RegionActivityRepository extends ElasticsearchRepository<RegionActivity, UUID> {
    List<RegionActivity> findByRegionIdOrderByActivityDateDesc(UUID regionId);
}
