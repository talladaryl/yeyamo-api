package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.CatalogAssetDimension;

public interface CatalogAssetDimensionRepository extends ElasticsearchRepository<CatalogAssetDimension, UUID> {
}
