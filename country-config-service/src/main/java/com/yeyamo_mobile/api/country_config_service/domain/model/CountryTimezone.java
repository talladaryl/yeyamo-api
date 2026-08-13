package com.yeyamo_mobile.api.country_config_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Timezones for countries that span multiple zones.
 * Most African countries have a single timezone.
 */
@Entity
@Table(name = "country_timezones", indexes = {
    @Index(name = "idx_country_tz_country_id", columnList = "countryId")
})
public class CountryTimezone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID countryId;

    /**
     * IANA timezone identifier (e.g., "Africa/Douala")
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String timezone;

    /**
     * Display name (e.g., "West Africa Time (WAT)")
     */
    @Size(max = 100)
    @Column(length = 100)
    private String displayName;

    /**
     * Is this the default timezone for the country?
     */
    @NotNull
    @Column(nullable = false)
    private Boolean isDefault = false;

    // Constructors
    protected CountryTimezone() {
    }

    public CountryTimezone(UUID countryId, String timezone, String displayName, Boolean isDefault) {
        this.countryId = countryId;
        this.timezone = timezone;
        this.displayName = displayName;
        this.isDefault = isDefault;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public UUID getCountryId() {
        return countryId;
    }

    public void setCountryId(UUID countryId) {
        this.countryId = countryId;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
