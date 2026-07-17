package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.UUID;
import com.yeyamo_mobile.api.catalog_service.application.CollectionService;

public record CollectionSummaryResponse(
    UUID id,
    String title,
    UUID coverAssetId,
    long placeCount
) {
    public static CollectionSummaryResponse from(CollectionService.CollectionSummary summary) {
        return new CollectionSummaryResponse(
            summary.id(),
            summary.title(),
            summary.coverAssetId(),
            summary.placeCount()
        );
    }
}
