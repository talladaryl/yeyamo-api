package com.yeyamo_mobile.api.content_service.interfaces.rest;

import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

public record StoryRequest(
    @NotNull(message = "L'ID du média est obligatoire")
    UUID mediaId,
    
    @Size(max = 500, message = "La légende ne peut pas dépasser 500 caractères")
    String caption,
    
    @Min(value = 5, message = "La durée minimale est de 5 secondes")
    @Max(value = 60, message = "La durée maximale est de 60 secondes")
    Integer durationSeconds,
    @Pattern(regexp = "[A-Z]{2}", message = "countryCode must be ISO 3166-1 alpha-2") String countryCode,
    UUID adminLevel1Id,
    UUID adminLevel2Id,
    UUID cityId,
    UUID localityId,
    @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @Size(max = 10) String languageCode
) {}
