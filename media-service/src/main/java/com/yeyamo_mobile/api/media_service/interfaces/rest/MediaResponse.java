package com.yeyamo_mobile.api.media_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.media_service.domain.model.*;
public record MediaResponse(
 UUID id, String ownerId, MediaType type, MediaStatus status, ThumbnailStatus thumbnailStatus,
 String originalFilename, String contentType, long sizeBytes,
 String checksum, Integer width, Integer height, Long durationMs,
 String altText, String aggregateType, String aggregateId,
 String contentUrl, String thumbnailUrl, Instant createdAt, Instant updatedAt,
 // Culture & Artisan extensions
 MediaUsageType usageType,
 String copyrightOwner, String licenseType, String usagePermission,
 Boolean attributionRequired, ConsentStatus consentStatus
) {
 public static MediaResponse from(MediaAsset m){
  String base="/api/v1/media/"+m.getId();
  return new MediaResponse(
   m.getId(),m.getOwnerId(),m.getType(),m.getStatus(),m.getThumbnailStatus(),
   m.getOriginalFilename(),m.getContentType(),m.getSizeBytes(),m.getChecksum(),
   m.getWidth(),m.getHeight(),m.getDurationMs(),m.getAltText(),m.getAggregateType(),m.getAggregateId(),
   base+"/content",m.getThumbnailKey()==null?null:base+"/thumbnail",m.getCreatedAt(),m.getUpdatedAt(),
   m.getUsageType(),m.getCopyrightOwner(),m.getLicenseType(),m.getUsagePermission(),
   m.getAttributionRequired(),m.getConsentStatus());
 }
}
