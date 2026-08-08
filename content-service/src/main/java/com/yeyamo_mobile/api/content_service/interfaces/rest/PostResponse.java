package com.yeyamo_mobile.api.content_service.interfaces.rest;
import java.time.Instant;import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.*;
public record PostResponse(UUID id,String authorId,String caption,PostStatus status,PostVisibility visibility,UUID catalogAssetId,PostReferenceType referenceType,String referenceId,List<UUID> mediaIds,Set<String> hashtags,
 Instant createdAt,Instant updatedAt,Instant publishedAt,Instant archivedAt,long version){
 public static PostResponse from(Post p){return new PostResponse(p.getId(),p.getAuthorId(),p.getCaption(),p.getStatus(),p.getVisibility(),p.getCatalogAssetId(),p.getReferenceType(),p.getReferenceId(),p.getMediaIds(),p.getHashtags(),
  p.getCreatedAt(),p.getUpdatedAt(),p.getPublishedAt(),p.getArchivedAt(),p.getVersion());}
}
