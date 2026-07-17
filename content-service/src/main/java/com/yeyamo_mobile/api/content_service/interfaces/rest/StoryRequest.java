package com.yeyamo_mobile.api.content_service.interfaces.rest;

import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoryRequest(
    @NotNull(message = "L'ID du média est obligatoire")
    UUID mediaId,
    
    @Size(max = 500, message = "La légende ne peut pas dépasser 500 caractères")
    String caption,
    
    @Min(value = 5, message = "La durée minimale est de 5 secondes")
    @Max(value = 60, message = "La durée maximale est de 60 secondes")
    Integer durationSeconds
) {}
