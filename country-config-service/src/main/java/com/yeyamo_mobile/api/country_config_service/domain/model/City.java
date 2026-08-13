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
 * City entity - major urban centers within administrative areas.
 * 
 * Links to the generic administrative_areas hierarchy.
 */
@Entity
@Table(name = "cities", indexes = {
    @Index(name = "idx_city_country", columnList = "countryCode"),
    @Index(name = "idx_city_admin_area", columnList = "administrativeAreaId"),
    @Index(name = "idx_city_slug", columnList = "slug"),
    @Index(name = "idx_city_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
public class City {

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
     * Parent administrative area (e.g., department, LGA)
     */
    @Column(nullable = false)
    private UUID administrativeAreaId;

    /**
     * City name
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
     * Latitude
     */
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * Longitude
     */
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * Is this city active?
     */
    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Population (optional)
     */
    @Column
    private Long population;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Constructors
    protected City() {
    }

    public City(
            String countryCode,
            UUID administrativeAreaId,
            String name,
            String slug
    ) {
        this.countryCode = countryCode;
        this.administrativeAreaId = administrativeAreaId;
        this.name = name;
        this.slug = slug;
    }

    // Business methods
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

    public UUID getAdministrativeAreaId() {
        return administrativeAreaId;
    }

    public void setAdministrativeAreaId(UUID administrativeAreaId) {
        this.administrativeAreaId = administrativeAreaId;
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

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
