package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionRequest(
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 120, message = "Le titre ne peut pas dépasser 120 caractères")
    String title,
    
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    String description,
    
    Boolean isPublic,
    
    UUID coverAssetId
) {}
