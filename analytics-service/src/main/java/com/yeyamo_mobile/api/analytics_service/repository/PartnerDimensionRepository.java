package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.PartnerDimension;

public interface PartnerDimensionRepository extends ElasticsearchRepository<PartnerDimension, UUID> {
    Optional<PartnerDimension> findByOwnerUserId(String ownerUserId);
}
