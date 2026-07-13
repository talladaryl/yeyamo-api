package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogReference;
import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;

public record CatalogReferenceResponse(UUID id, ReferenceType type, String code, String name,
        String parentCode, String countryCode, String description, boolean active,
        Instant createdAt, Instant updatedAt, long version) {
    static CatalogReferenceResponse from(CatalogReference value) {
        return new CatalogReferenceResponse(value.getId(), value.getType(), value.getCode(), value.getName(),
                value.getParentCode(), value.getCountryCode(), value.getDescription(), value.isActive(),
                value.getCreatedAt(), value.getUpdatedAt(), value.getVersion());
    }
}
