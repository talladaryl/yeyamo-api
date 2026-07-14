package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CatalogReferenceRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*") String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 80) String parentCode,
        @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
        @Size(max = 5000) String description) {
}
