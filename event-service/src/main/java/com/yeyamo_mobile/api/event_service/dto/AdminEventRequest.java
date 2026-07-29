package com.yeyamo_mobile.api.event_service.dto;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.event_service.enums.EventStatus;import jakarta.validation.constraints.*;
public record AdminEventRequest(@NotNull UUID placeId,UUID organizerId,UUID partnerId,Long regionId,Long cityId,Long categoryId,@NotBlank@Size(max=255)String title,@Size(max=10000)String description,@NotNull Instant startAt,@NotNull Instant endAt,@NotNull@Min(1)Integer capacity,EventStatus status){}
