package com.yeyamo_mobile.shared.geography;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable geographic fields for multi-country support.
 * 
 * Can be used as @Embedded in entities that need geographic location.
 */
@Embeddable
public class GeographicFields {

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "admin_level_1_id")
    private UUID adminLevel1Id;

    @Column(name = "admin_level_2_id")
    private UUID adminLevel2Id;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "locality_id")
    private UUID localityId;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    protected GeographicFields() {
    }

    public GeographicFields(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("countryCode is required");
        }
        this.countryCode = countryCode;
    }

    public void setLocation(UUID adminLevel1Id, UUID adminLevel2Id, UUID cityId, UUID localityId) {
        this.adminLevel1Id = adminLevel1Id;
        this.adminLevel2Id = adminLevel2Id;
        this.cityId = cityId;
        this.localityId = localityId;
    }

    public void setCoordinates(Double latitude, Double longitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    // Getters and Setters
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public UUID getAdminLevel1Id() {
        return adminLevel1Id;
    }

    public void setAdminLevel1Id(UUID adminLevel1Id) {
        this.adminLevel1Id = adminLevel1Id;
    }

    public UUID getAdminLevel2Id() {
        return adminLevel2Id;
    }

    public void setAdminLevel2Id(UUID adminLevel2Id) {
        this.adminLevel2Id = adminLevel2Id;
    }

    public UUID getCityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public UUID getLocalityId() {
        return localityId;
    }

    public void setLocalityId(UUID localityId) {
        this.localityId = localityId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}
