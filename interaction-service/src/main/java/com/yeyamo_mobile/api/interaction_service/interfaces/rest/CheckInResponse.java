package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.interaction_service.domain.model.CheckIn;
public record CheckInResponse(UUID id,UUID catalogAssetId,String userId,Double latitude,Double longitude,boolean visible,Instant occurredAt){public static CheckInResponse from(CheckIn c){return new CheckInResponse(c.id(),c.catalogAssetId(),c.userId(),c.latitude(),c.longitude(),c.visible(),c.occurredAt());}}
