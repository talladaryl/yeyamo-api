package com.yeyamo_mobile.api.discovery_service.infrastructure.postgis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "discovery_documents")
public class DiscoveryDocumentEntity {
    @Id
    private UUID id;

    @Column(name = "source_id", nullable = false, unique = true, length = 120)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscoveryType type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_code", length = 100)
    private String categoryCode;

    @Column(name = "region_code", length = 40)
    private String regionCode;

    @Column(length = 160)
    private String city;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "admin_level1_id", length = 120)
    private String adminLevel1Id;

    @Column(name = "city_id", length = 120)
    private String cityId;

    @Column(name = "translated_titles", columnDefinition = "TEXT")
    private String translatedTitlesJson;

    @Column(name = "language_codes", length = 500)
    private String languageCodes;

    @Column(length = 300)
    private String community;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String materials;

    @Column(columnDefinition = "TEXT")
    private String techniques;

    @Column(name = "artisan_id", length = 120)
    private String artisanId;

    @Column(name = "verification_status", length = 30)
    private String verificationStatus;

    @Column(name = "availability_status", length = 30)
    private String availabilityStatus;

    @Column(name = "price_min", precision = 12, scale = 2)
    private BigDecimal priceMin;

    @Column(name = "price_max", precision = 12, scale = 2)
    private BigDecimal priceMax;

    @Column(name = "popularity_signal", nullable = false)
    private double popularitySignal;

    @Column(name = "launch_visibility", nullable = false, length = 30)
    private String launchVisibility = "PUBLIC";

    @Column(columnDefinition = "TEXT")
    private String aliases;

    private Double latitude;
    private Double longitude;

    @Column(name = "author_id", length = 120)
    private String authorId;

    @Column(name = "trend_score", nullable = false)
    private double trendScore;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String value) { sourceId = value; }
    public DiscoveryType getType() { return type; }
    public void setType(DiscoveryType value) { type = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String value) { categoryCode = value; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String value) { regionCode = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String value) { countryCode = value; }
    public String getAdminLevel1Id() { return adminLevel1Id; }
    public void setAdminLevel1Id(String value) { adminLevel1Id = value; }
    public String getCityId() { return cityId; }
    public void setCityId(String value) { cityId = value; }
    public String getTranslatedTitlesJson() { return translatedTitlesJson; }
    public void setTranslatedTitlesJson(String value) { translatedTitlesJson = value; }
    public String getLanguageCodes() { return languageCodes; }
    public void setLanguageCodes(String value) { languageCodes = value; }
    public String getCommunity() { return community; }
    public void setCommunity(String value) { community = value; }
    public String getTags() { return tags; }
    public void setTags(String value) { tags = value; }
    public String getMaterials() { return materials; }
    public void setMaterials(String value) { materials = value; }
    public String getTechniques() { return techniques; }
    public void setTechniques(String value) { techniques = value; }
    public String getArtisanId() { return artisanId; }
    public void setArtisanId(String value) { artisanId = value; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String value) { verificationStatus = value; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String value) { availabilityStatus = value; }
    public BigDecimal getPriceMin() { return priceMin; }
    public void setPriceMin(BigDecimal value) { priceMin = value; }
    public BigDecimal getPriceMax() { return priceMax; }
    public void setPriceMax(BigDecimal value) { priceMax = value; }
    public double getPopularitySignal() { return popularitySignal; }
    public void setPopularitySignal(double value) { popularitySignal = value; }
    public String getLaunchVisibility() { return launchVisibility; }
    public void setLaunchVisibility(String value) { launchVisibility = value; }
    public String getAliases() { return aliases; }
    public void setAliases(String value) { aliases = value; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double value) { latitude = value; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double value) { longitude = value; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String value) { authorId = value; }
    public double getTrendScore() { return trendScore; }
    public void setTrendScore(double value) { trendScore = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant value) { publishedAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
