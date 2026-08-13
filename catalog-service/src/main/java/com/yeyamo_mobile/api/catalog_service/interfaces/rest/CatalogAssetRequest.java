package com.yeyamo_mobile.api.catalog_service.interfaces.rest;
import java.util.UUID;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;
import jakarta.validation.constraints.*;
public record CatalogAssetRequest(
        @NotNull AssetType type,
        UUID ownerId,
        @NotBlank @Size(max=200) String name,
        @Size(max=220) String slug,
        @Size(max=10000) String description,
        @Size(max=100) String categoryCode,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "countryCode doit Ãªtre ISO-2") String countryCode,
        @Size(max=40) String regionCode,
        @Size(max=160) String city,
        @Size(max=160) String district,
        @Size(max=300) String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
){
    public CatalogAssetRequest(AssetType type, UUID ownerId, String name, String slug, String description,
            String categoryCode, String regionCode, String city, String district, String address,
            Double latitude, Double longitude) {
        this(type, ownerId, name, slug, description, categoryCode, null, regionCode, city, district, address,
                latitude, longitude);
    }
}
