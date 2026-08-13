package com.yeyamo_mobile.api.country_config_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Locality entity - smallest administrative unit (neighborhoods, villages, zones).
 * 
 * Examples:
 * - Quartiers (Cameroon neighborhoods)
 * - Villages
 * - Wards (Nigeria)
 * - Suburbs
 * - Local zones
 */
@Entity
@Table(name = "localities", indexes = {
    @Index(name = "idx_locality_country", columnList = "countryCode"),
    @Index(name = "idx_locality_city", columnList = "cityId"),
    @Index(name = "idx_locality_admin_area", columnList = "administrativeAreaId"),
    @Index(name = "idx_locality_slug", columnList = "slug"),
    @Index(name = "idx_locality_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
public class Locality {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Country code (ISO 3166-1 alpha-2)
     */
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}")
    @Column(nullable = false, length = 2)
    private String countryCode;

    /**
     * Parent city (if urban locality)
     */
    @Column
    private UUID cityId;

    /**
     * Parent administrative area (if rural or not attached to a city)
     */
    @Column
    private UUID administrativeAreaId;

    /**
     * Type of locality
     * Examples: "quartier", "village", "ward", "suburb", "zone"
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String localityType;

    /**
     * Locality name
     */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * URL-friendly slug
     */
    @NotBlank
    @Size(max = 250)
    @Column(nullable = false, length = 250)
    private String slug;

    /**
     * Latitude (optional)
     */
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * Longitude (optional)
     */
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * Is this locality active?
     */
    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Constructors
    protected Locality() {
    }

    public Locality(
            String countryCode,
            UUID cityId,
            UUID administrativeAreaId,
            String localityType,
            String name,
            String slug
    ) {
        this.countryCode = countryCode;
        this.cityId = cityId;
        this.administrativeAreaId = administrativeAreaId;
        this.localityType = localityType;
        this.name = name;
        this.slug = slug;
    }

    // Business methods
    public boolean isUrban() {
        return cityId != null;
    }

    public boolean isRural() {
        return cityId == null && administrativeAreaId != null;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public UUID getCityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public UUID getAdministrativeAreaId() {
        return administrativeAreaId;
    }

    public void setAdministrativeAreaId(UUID administrativeAreaId) {
        this.administrativeAreaId = administrativeAreaId;
    }

    public String getLocalityType() {
        return localityType;
    }

    public void setLocalityType(String localityType) {
        this.localityType = localityType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
