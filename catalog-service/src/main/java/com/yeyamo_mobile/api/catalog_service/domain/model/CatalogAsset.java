package com.yeyamo_mobile.api.catalog_service.domain.model;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    private String countryCode;
    private String regionCode;
    private String city;
    private String district;
    private String address;
    private GeoPoint location;
    private List<UUID> mediaIds = new ArrayList<>();
    private Integer durationMinutes;
    private String difficultyLevel;
    private BigDecimal price;
    private String currency;
    private Integer capacityMin;
    private Integer capacityMax;
    private List<String> includedItems = new ArrayList<>();
    private List<String> excludedItems = new ArrayList<>();
    private UUID placeId;
    private AssetStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public static CatalogAsset create(AssetType type, UUID ownerId, String source, String externalId,
            String name, String slug, String description, String categoryCode, String regionCode,
            String city, String district, String address, GeoPoint location) {
        return create(type, ownerId, source, externalId, name, slug, description, categoryCode,
                null, regionCode, city, district, address, location);
    }

    public static CatalogAsset create(AssetType type, UUID ownerId, String source, String externalId,
            String name, String slug, String description, String categoryCode, String countryCode, String regionCode,
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
        asset.countryCode = trimToNull(countryCode);
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
        update(name, slug, description, categoryCode, null, regionCode, city, district, address, location);
    }

    public void update(String name, String slug, String description, String categoryCode, String countryCode,
            String regionCode, String city, String district, String address, GeoPoint location) {
        this.name = requireText(name, "name");
        this.slug = requireText(slug, "slug");
        this.description = trimToNull(description);
        this.categoryCode = trimToNull(categoryCode);
        if (countryCode != null) this.countryCode = trimToNull(countryCode);
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
            case DELETED -> false;
        };
        if (!allowed || target == status) {
            throw new IllegalStateException("Invalid catalog status transition: " + status + " -> " + target);
        }
        status = target;
        updatedAt = Instant.now();
    }

    public void enrich(List<UUID> mediaIds, Integer durationMinutes, String difficultyLevel,
            BigDecimal price, String currency, Integer capacityMin, Integer capacityMax,
            List<String> includedItems, List<String> excludedItems, UUID placeId) {
        if (durationMinutes != null && durationMinutes < 0) throw new IllegalArgumentException("durationMinutes must be positive");
        if (price != null && price.signum() <= 0) throw new IllegalArgumentException("price must be positive");
        if ((price == null) != (trimToNull(currency) == null)) throw new IllegalArgumentException("price and currency must be supplied together");
        if (capacityMin != null && capacityMin < 0) throw new IllegalArgumentException("capacityMin must be positive");
        if (capacityMax != null && capacityMax < 0) throw new IllegalArgumentException("capacityMax must be positive");
        if (capacityMin != null && capacityMax != null && capacityMin > capacityMax)
            throw new IllegalArgumentException("capacityMin must be less than or equal to capacityMax");
        this.mediaIds = copyUuidList(mediaIds);
        this.durationMinutes = durationMinutes;
        this.difficultyLevel = normalizeDifficulty(difficultyLevel);
        this.price = price;
        this.currency = trimToNull(currency) == null ? null : currency.trim().toUpperCase(java.util.Locale.ROOT);
        this.capacityMin = capacityMin;
        this.capacityMax = capacityMax;
        this.includedItems = copyTextList(includedItems);
        this.excludedItems = copyTextList(excludedItems);
        this.placeId = placeId;
        this.updatedAt = Instant.now();
    }

    public void delete() {
        if (status == AssetStatus.DELETED) return;
        status = AssetStatus.DELETED;
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
    private static List<UUID> copyUuidList(List<UUID> values) { return values == null ? new ArrayList<>() : new ArrayList<>(values); }
    private static List<String> copyTextList(List<String> values) {
        if (values == null) return new ArrayList<>();
        return values.stream().map(CatalogAsset::trimToNull).filter(java.util.Objects::nonNull).toList();
    }
    private static String normalizeDifficulty(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT").contains(normalized))
            throw new IllegalArgumentException("difficultyLevel must be BEGINNER, INTERMEDIATE, ADVANCED or EXPERT");
        return normalized;
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
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode == null ? null : countryCode.trim().toUpperCase(java.util.Locale.ROOT); }
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
    public List<UUID> getMediaIds() { return List.copyOf(mediaIds); }
    public void setMediaIds(List<UUID> mediaIds) { this.mediaIds = copyUuidList(mediaIds); }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = normalizeDifficulty(difficultyLevel); }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getCapacityMin() { return capacityMin; }
    public void setCapacityMin(Integer capacityMin) { this.capacityMin = capacityMin; }
    public Integer getCapacityMax() { return capacityMax; }
    public void setCapacityMax(Integer capacityMax) { this.capacityMax = capacityMax; }
    public List<String> getIncludedItems() { return List.copyOf(includedItems); }
    public void setIncludedItems(List<String> includedItems) { this.includedItems = copyTextList(includedItems); }
    public List<String> getExcludedItems() { return List.copyOf(excludedItems); }
    public void setExcludedItems(List<String> excludedItems) { this.excludedItems = copyTextList(excludedItems); }
    public UUID getPlaceId() { return placeId; }
    public void setPlaceId(UUID placeId) { this.placeId = placeId; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
