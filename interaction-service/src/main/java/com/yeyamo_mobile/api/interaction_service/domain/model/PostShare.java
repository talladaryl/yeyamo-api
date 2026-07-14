package com.yeyamo_mobile.api.interaction_service.domain.model;
import java.time.Instant;import java.util.UUID;
public record PostShare(UUID id,UUID postId,String userId,String channel,Instant createdAt){
 public static PostShare create(UUID postId,String userId,String channel){if(postId==null)throw new IllegalArgumentException("postId is required");if(userId==null||userId.isBlank())throw new IllegalArgumentException("userId is required");String normalized=channel==null||channel.isBlank()?"INTERNAL":channel.trim().toUpperCase(java.util.Locale.ROOT);
  if(normalized.length()>40)throw new IllegalArgumentException("share channel is invalid");return new PostShare(UUID.randomUUID(),postId,userId,normalized,Instant.now());}
}
