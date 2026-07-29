package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import jakarta.validation.constraints.Size;

public record UpdateCollectionPlaceRequest(
        Boolean isPriority,
        Integer displayOrder,
        @Size(max = 1000) String note) {
}
