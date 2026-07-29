package com.yeyamo_mobile.api.catalog_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CatalogAssetEntity;
public record AdminCatalogAssetResponse(UUID id,String type,UUID ownerId,String source,String externalId,String name,String slug,String description,String categoryCode,String regionCode,String city,String district,String address,double latitude,double longitude,String status,Instant createdAt,Instant updatedAt){
 public static AdminCatalogAssetResponse from(CatalogAssetEntity a){return new AdminCatalogAssetResponse(a.getId(),a.getType().name(),a.getOwnerId(),a.getSource(),a.getExternalId(),a.getName(),a.getSlug(),a.getDescription(),a.getCategoryCode(),a.getRegionCode(),a.getCity(),a.getDistrict(),a.getAddress(),a.getLatitude(),a.getLongitude(),a.getStatus().name(),a.getCreatedAt(),a.getUpdatedAt());}
}
