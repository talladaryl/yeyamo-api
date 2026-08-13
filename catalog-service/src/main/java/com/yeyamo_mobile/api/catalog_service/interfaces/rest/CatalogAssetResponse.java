package com.yeyamo_mobile.api.catalog_service.interfaces.rest;
import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.catalog_service.domain.model.*;
public record CatalogAssetResponse(UUID id,AssetType type,UUID ownerId,String source,String externalId,
        String name,String slug,String description,String categoryCode,String countryCode,String regionCode,String city,
        String district,String address,double latitude,double longitude,AssetStatus status,
        Instant createdAt,Instant updatedAt,long version){
    public static CatalogAssetResponse from(CatalogAsset a){return new CatalogAssetResponse(a.getId(),a.getType(),
            a.getOwnerId(),a.getSource(),a.getExternalId(),a.getName(),a.getSlug(),a.getDescription(),
            a.getCategoryCode(),a.getCountryCode(),a.getRegionCode(),a.getCity(),a.getDistrict(),a.getAddress(),
            a.getLocation().latitude(),a.getLocation().longitude(),a.getStatus(),a.getCreatedAt(),a.getUpdatedAt(),a.getVersion());}
}
