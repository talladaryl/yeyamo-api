package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.yeyamo_mobile.api.catalog_service.application.CollectionService;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CollectionEntity;

public record CollectionResponse(
    UUID id,
    UUID userId,
    String title,
    String description,
    boolean isPublic,
    UUID coverAssetId,
    Instant createdAt,
    Instant updatedAt,
    List<CatalogAssetResponse> places
) {
    public static CollectionResponse from(CollectionEntity collection) {
        return new CollectionResponse(
            collection.getId(),
            collection.getUserId(),
            collection.getTitle(),
            collection.getDescription(),
            collection.isPublic(),
            collection.getCoverAssetId(),
            collection.getCreatedAt(),
            collection.getUpdatedAt(),
            null // Pas de lieux pour la liste simple
        );
    }

    public static CollectionResponse fromWithAssets(CollectionService.CollectionWithAssets data) {
        CollectionEntity collection = data.collection();
        List<CatalogAssetResponse> places = data.assets().stream()
                .map(CatalogAssetResponse::from)
                .toList();
        
        return new CollectionResponse(
            collection.getId(),
            collection.getUserId(),
            collection.getTitle(),
            collection.getDescription(),
            collection.isPublic(),
            collection.getCoverAssetId(),
            collection.getCreatedAt(),
            collection.getUpdatedAt(),
            places
        );
    }
}
