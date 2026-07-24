package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddPlaceRequest(
    @NotNull(message = "L'ID de la collection est obligatoire")
    UUID collectionId,
    
    @NotNull(message = "L'ID de l'asset est obligatoire")
    UUID assetId,

    Boolean isPriority,

    @Size(max = 1000, message = "La note ne peut pas dépasser 1000 caractères")
    String note
) {}
