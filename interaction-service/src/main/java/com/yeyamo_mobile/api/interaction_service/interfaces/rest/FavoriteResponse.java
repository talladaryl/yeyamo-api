package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.interaction_service.domain.model.PostRelation;
public record FavoriteResponse(UUID postId,Instant savedAt){public static FavoriteResponse from(PostRelation r){return new FavoriteResponse(r.postId(),r.createdAt());}}
