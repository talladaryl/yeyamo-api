package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.Set;
import java.util.UUID;
import com.yeyamo_mobile.api.catalog_service.domain.model.CollectionScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionRequest(
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 120, message = "Le titre ne peut pas dépasser 120 caractères")
    String title,
    
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    String description,
    
    Boolean isPublic,
    
    UUID coverAssetId,

    Set<@Size(min = 2, max = 2, message = "Chaque pays doit Ãªtre un code ISO-2") String> targetCountries,

    Set<@Size(min = 2, max = 10, message = "Chaque langue doit Ãªtre un code BCP-47 court") String> targetLanguages,

    CollectionScope scope
) {}
