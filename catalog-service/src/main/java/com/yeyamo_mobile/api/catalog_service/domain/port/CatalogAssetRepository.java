package com.yeyamo_mobile.api.catalog_service.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;

public interface CatalogAssetRepository {
    CatalogAsset save(CatalogAsset asset);
    Optional<CatalogAsset> findById(UUID id);
    boolean existsById(UUID id);
    List<CatalogAsset> findAllById(List<UUID> ids);
    Optional<CatalogAsset> findBySlug(String slug);
    Optional<CatalogAsset> findBySourceAndExternalId(String source, String externalId);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    List<CatalogAsset> search(AssetStatus status, AssetType type, String regionCode,
            String categoryCode, String query, int limit);
    List<CatalogAsset> findNearby(double latitude, double longitude, double radiusMeters,
            AssetType type, String categoryCode, int limit);
}
