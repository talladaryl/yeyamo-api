package com.yeyamo_mobile.api.feed_service.domain.model;
import java.time.Instant;import java.util.*;
public record FeedPost(UUID postId,String authorId,String caption,String visibility,String status,UUID catalogAssetId,String referenceType,String referenceId,List<UUID>mediaIds,List<String>hashtags,Instant publishedAt,Instant updatedAt){
 public FeedPost{if(postId==null)throw new IllegalArgumentException("postId is required");if(authorId==null||authorId.isBlank())throw new IllegalArgumentException("authorId is required");referenceType=referenceType==null?"NONE":referenceType;mediaIds=mediaIds==null?List.of():List.copyOf(mediaIds);hashtags=hashtags==null?List.of():List.copyOf(hashtags);}
 public FeedPost(UUID postId,String authorId,String caption,String visibility,String status,UUID catalogAssetId,List<UUID>mediaIds,List<String>hashtags,Instant publishedAt,Instant updatedAt){this(postId,authorId,caption,visibility,status,catalogAssetId,catalogAssetId==null?"NONE":"PLACE",catalogAssetId==null?null:catalogAssetId.toString(),mediaIds,hashtags,publishedAt,updatedAt);}
 public boolean available(){return "PUBLISHED".equals(status)&&"PUBLIC".equals(visibility)&&publishedAt!=null;}
}
