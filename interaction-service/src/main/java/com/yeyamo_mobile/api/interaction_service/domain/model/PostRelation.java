package com.yeyamo_mobile.api.interaction_service.domain.model;
import java.time.Instant;import java.util.UUID;
public record PostRelation(UUID id,UUID postId,String userId,RelationType type,Instant createdAt){
 public static PostRelation create(UUID postId,String userId,RelationType type){if(postId==null)throw new IllegalArgumentException("postId is required");if(userId==null||userId.isBlank())throw new IllegalArgumentException("userId is required");if(type==null)throw new IllegalArgumentException("relation type is required");
  return new PostRelation(UUID.randomUUID(),postId,userId,type,Instant.now());}
}
