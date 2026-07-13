package com.yeyamo_mobile.api.catalog_service.domain.model;

import java.time.Instant;
import java.util.UUID;

public class CatalogAsset {
    private UUID id;
    private AssetType type;
    private UUID ownerId;
    private String source;
    private String externalId;
    private String name;
    private String slug;
    private String description;
    private String categoryCode;
    private String regionCode;
    private String city;
    private String district;
    private String address;
    private GeoPoint location;
    private AssetStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public static CatalogAsset create(AssetType type, UUID ownerId, String source, String externalId,
            String name, String slug, String description, String categoryCode, String regionCode,
            String city, String district, String address, GeoPoint location) {
        CatalogAsset asset = new CatalogAsset();
        asset.id = UUID.randomUUID();
        asset.type = require(type, "type");
        asset.ownerId = ownerId;
        asset.source = normalize(source, "catalog");
        asset.externalId = trimToNull(externalId);
        asset.name = requireText(name, "name");
        asset.slug = requireText(slug, "slug");
        asset.description = trimToNull(description);
        asset.categoryCode = trimToNull(categoryCode);
        asset.regionCode = trimToNull(regionCode);
        asset.city = trimToNull(city);
        asset.district = trimToNull(district);
        asset.address = trimToNull(address);
        asset.location = require(location, "location");
        asset.status = AssetStatus.DRAFT;
        asset.createdAt = Instant.now();
        asset.updatedAt = asset.createdAt;
        return asset;
    }

    public void update(String name, String slug, String description, String categoryCode,
            String regionCode, String city, String district, String address, GeoPoint location) {
        this.name = requireText(name, "name");
        this.slug = requireText(slug, "slug");
        this.description = trimToNull(description);
        this.categoryCode = trimToNull(categoryCode);
        this.regionCode = trimToNull(regionCode);
        this.city = trimToNull(city);
        this.district = trimToNull(district);
        this.address = trimToNull(address);
        this.location = require(location, "location");
        this.updatedAt = Instant.now();
    }

    public void changeStatus(AssetStatus target) {
        require(target, "status");
        boolean allowed = switch (status) {
            case DRAFT -> target == AssetStatus.IN_REVIEW || target == AssetStatus.ARCHIVED;
            case IN_REVIEW -> target == AssetStatus.DRAFT || target == AssetStatus.PUBLISHED || target == AssetStatus.ARCHIVED;
            case PUBLISHED -> target == AssetStatus.IN_REVIEW || target == AssetStatus.ARCHIVED;
            case ARCHIVED -> target == AssetStatus.DRAFT;
        };
        if (!allowed || target == status) {
            throw new IllegalStateException("Invalid catalog status transition: " + status + " -> " + target);
        }
        status = target;
        updatedAt = Instant.now();
    }

    public void synchronizeStatus(AssetStatus target) {
        status = target == null ? AssetStatus.DRAFT : target;
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String name) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(name + " is required");
        return normalized;
    }
    private static String normalize(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }
    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }
    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
