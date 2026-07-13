package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.interaction_service.domain.model.PostShare;
public record ShareResponse(UUID id,UUID postId,String userId,String channel,Instant createdAt){public static ShareResponse from(PostShare s){return new ShareResponse(s.id(),s.postId(),s.userId(),s.channel(),s.createdAt());}}
