package com.yeyamo_mobile.api.catalog_service.interfaces.rest;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.yeyamo_mobile.api.catalog_service.domain.model.*;
public record CatalogAssetResponse(UUID id,AssetType type,UUID ownerId,String source,String externalId,
        String name,String slug,String description,String categoryCode,String countryCode,String regionCode,String city,
        String district,String address,double latitude,double longitude,AssetStatus status,
        Instant createdAt,Instant updatedAt,long version,List<UUID> mediaIds,Integer durationMinutes,String difficultyLevel,
        BigDecimal price,String currency,Integer capacityMin,Integer capacityMax,List<String> includedItems,
        List<String> excludedItems,UUID placeId){
    public static CatalogAssetResponse from(CatalogAsset a){return new CatalogAssetResponse(a.getId(),a.getType(),
            a.getOwnerId(),a.getSource(),a.getExternalId(),a.getName(),a.getSlug(),a.getDescription(),
            a.getCategoryCode(),a.getCountryCode(),a.getRegionCode(),a.getCity(),a.getDistrict(),a.getAddress(),
            a.getLocation().latitude(),a.getLocation().longitude(),a.getStatus(),a.getCreatedAt(),a.getUpdatedAt(),a.getVersion(),
            a.getMediaIds(),a.getDurationMinutes(),a.getDifficultyLevel(),a.getPrice(),a.getCurrency(),a.getCapacityMin(),
            a.getCapacityMax(),a.getIncludedItems(),a.getExcludedItems(),a.getPlaceId());}
}
