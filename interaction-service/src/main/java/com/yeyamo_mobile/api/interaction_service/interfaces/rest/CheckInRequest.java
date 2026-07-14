package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.util.UUID;import jakarta.validation.constraints.*;
public record CheckInRequest(@NotNull UUID catalogAssetId,@DecimalMin("-90")@DecimalMax("90")Double latitude,@DecimalMin("-180")@DecimalMax("180")Double longitude,boolean visible){}
