package com.yeyamo_mobile.api.content_service.interfaces.rest;
import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.*;import jakarta.validation.constraints.*;
public record PostRequest(@Size(max=5000)String caption,PostVisibility visibility,UUID catalogAssetId,@Size(max=10)List<UUID> mediaIds,@Size(max=20)Set<@Size(max=51)String> hashtags,PostReferenceType referenceType,@Size(max=100)String referenceId,
 @Pattern(regexp="[A-Z]{2}",message="countryCode must be ISO 3166-1 alpha-2")String countryCode,UUID adminLevel1Id,UUID adminLevel2Id,UUID cityId,UUID localityId,
 @DecimalMin("-90.0")@DecimalMax("90.0")Double latitude,@DecimalMin("-180.0")@DecimalMax("180.0")Double longitude,@Size(max=10)String languageCode){}
