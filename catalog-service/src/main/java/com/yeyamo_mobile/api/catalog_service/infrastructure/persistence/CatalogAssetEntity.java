package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Point;

import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "catalog_assets")
public class CatalogAssetEntity {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AssetType type;
    @Column(name = "owner_id") private UUID ownerId;
    @Column(nullable = false, length = 80) private String source;
    @Column(name = "external_id", length = 160) private String externalId;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, unique = true, length = 220) private String slug;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "category_code", length = 100) private String categoryCode;
    @Column(name = "country_code", length = 2) private String countryCode;
    @Column(name = "region_code", length = 40) private String regionCode;
    @Column(length = 160) private String city;
    @Column(length = 160) private String district;
    @Column(length = 300) private String address;
    private Double latitude;
    private Double longitude;
    @Column(columnDefinition = "geometry(Point,4326)") private Point location;
    @ElementCollection @CollectionTable(name = "catalog_asset_media", joinColumns = @JoinColumn(name = "catalog_asset_id"))
    @Column(name = "media_id", nullable = false) @OrderColumn(name = "display_order") private List<UUID> mediaIds = new ArrayList<>();
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(name = "difficulty_level", length = 32) private String difficultyLevel;
    @Column(precision = 19, scale = 2) private BigDecimal price;
    @Column(length = 3) private String currency;
    @Column(name = "capacity_min") private Integer capacityMin;
    @Column(name = "capacity_max") private Integer capacityMax;
    @ElementCollection @CollectionTable(name = "catalog_asset_included_items", joinColumns = @JoinColumn(name = "catalog_asset_id"))
    @Column(name = "item", nullable = false, length = 500) @OrderColumn(name = "display_order") private List<String> includedItems = new ArrayList<>();
    @ElementCollection @CollectionTable(name = "catalog_asset_excluded_items", joinColumns = @JoinColumn(name = "catalog_asset_id"))
    @Column(name = "item", nullable = false, length = 500) @OrderColumn(name = "display_order") private List<String> excludedItems = new ArrayList<>();
    @Column(name = "place_id") private UUID placeId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AssetStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public AssetType getType() { return type; } public void setType(AssetType type) { this.type = type; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getSource() { return source; } public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; } public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; } public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getCategoryCode() { return categoryCode; } public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getRegionCode() { return regionCode; } public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; } public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; } public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; } public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Point getLocation() { return location; } public void setLocation(Point location) { this.location = location; }
    public List<UUID> getMediaIds() { return mediaIds; } public void setMediaIds(List<UUID> mediaIds) { this.mediaIds = mediaIds == null ? new ArrayList<>() : new ArrayList<>(mediaIds); }
    public Integer getDurationMinutes() { return durationMinutes; } public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDifficultyLevel() { return difficultyLevel; } public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public Integer getCapacityMin() { return capacityMin; } public void setCapacityMin(Integer capacityMin) { this.capacityMin = capacityMin; }
    public Integer getCapacityMax() { return capacityMax; } public void setCapacityMax(Integer capacityMax) { this.capacityMax = capacityMax; }
    public List<String> getIncludedItems() { return includedItems; } public void setIncludedItems(List<String> includedItems) { this.includedItems = includedItems == null ? new ArrayList<>() : new ArrayList<>(includedItems); }
    public List<String> getExcludedItems() { return excludedItems; } public void setExcludedItems(List<String> excludedItems) { this.excludedItems = excludedItems == null ? new ArrayList<>() : new ArrayList<>(excludedItems); }
    public UUID getPlaceId() { return placeId; } public void setPlaceId(UUID placeId) { this.placeId = placeId; }
    public AssetStatus getStatus() { return status; } public void setStatus(AssetStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; } public void setVersion(long version) { this.version = version; }
}
