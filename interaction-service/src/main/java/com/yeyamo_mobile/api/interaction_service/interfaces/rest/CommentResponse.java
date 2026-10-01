package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.interaction_service.application.CommentLikeService.CommentLikeStatus;import com.yeyamo_mobile.api.interaction_service.domain.model.*;
public record CommentResponse(UUID id,UUID postId,UUID parentId,String authorId,String body,CommentStatus status,Instant createdAt,Instant updatedAt,long likeCount,boolean liked){
 public static CommentResponse from(Comment c){return new CommentResponse(c.getId(),c.getPostId(),c.getParentId(),c.getAuthorId(),c.getBody(),c.getStatus(),c.getCreatedAt(),c.getUpdatedAt(),0,false);}
 public static CommentResponse from(Comment c,CommentLikeStatus like){return new CommentResponse(c.getId(),c.getPostId(),c.getParentId(),c.getAuthorId(),c.getBody(),c.getStatus(),c.getCreatedAt(),c.getUpdatedAt(),like.likeCount(),like.liked());}}
