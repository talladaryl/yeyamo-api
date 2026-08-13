package com.yeyamo_mobile.api.country_config_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Country entity - central source of truth for country configuration.
 * 
 * Uses ISO standards:
 * - ISO 3166-1 alpha-2 for country codes
 * - ISO 4217 for currency codes
 * - ISO 639 / BCP 47 for language codes
 * - IANA for timezones
 * - E.164 for phone country codes
 */
@Entity
@Table(name = "countries", indexes = {
    @Index(name = "idx_country_code", columnList = "code", unique = true),
    @Index(name = "idx_country_launch_status", columnList = "launchStatus")
})
@EntityListeners(AuditingEntityListener.class)
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ISO 3166-1 alpha-2 country code (e.g., "CM" for Cameroon)
     */
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}", message = "Country code must be 2 uppercase letters (ISO 3166-1 alpha-2)")
    @Column(nullable = false, unique = true, length = 2)
    private String code;

    /**
     * Common country name (e.g., "Cameroon")
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Official country name (e.g., "Republic of Cameroon")
     */
    @Size(max = 200)
    @Column(length = 200)
    private String officialName;

    /**
     * Continent code: AF, NA, SA, AS, EU, OC, AN
     */
    @NotBlank
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String continentCode;

    /**
     * Default language code (ISO 639-1 or BCP 47, e.g., "fr" or "fr-CM")
     */
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String defaultLanguageCode;

    /**
     * Default currency code (ISO 4217, e.g., "XAF")
     */
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters (ISO 4217)")
    @Column(nullable = false, length = 3)
    private String defaultCurrencyCode;

    /**
     * Default timezone (IANA timezone, e.g., "Africa/Douala")
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String defaultTimezone;

    /**
     * Phone country code (E.164 format, e.g., "+237")
     */
    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = "\\+\\d{1,4}", message = "Phone country code must follow E.164 format (+XXX)")
    @Column(nullable = false, length = 5)
    private String phoneCountryCode;

    /**
     * Launch status: DISABLED, COMING_SOON, BETA, LIVE
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CountryLaunchStatus launchStatus;

    // Feature flags
    @NotNull
    @Column(nullable = false)
    private Boolean registrationEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean contentPublishingEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean partnerOnboardingEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean paymentsEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean bookingEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean ticketingEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean artisanCommerceEnabled = false;

    @NotNull
    @Column(nullable = false)
    private Boolean cultureModuleEnabled = false;

    // Auditing
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic locking for concurrent updates
     */
    @Version
    private Long version;

    // Constructors
    protected Country() {
    }

    public Country(
            String code,
            String name,
            String officialName,
            String continentCode,
            String defaultLanguageCode,
            String defaultCurrencyCode,
            String defaultTimezone,
            String phoneCountryCode,
            CountryLaunchStatus launchStatus
    ) {
        this.code = code;
        this.name = name;
        this.officialName = officialName;
        this.continentCode = continentCode;
        this.defaultLanguageCode = defaultLanguageCode;
        this.defaultCurrencyCode = defaultCurrencyCode;
        this.defaultTimezone = defaultTimezone;
        this.phoneCountryCode = phoneCountryCode;
        this.launchStatus = launchStatus;
    }

    // Business methods
    public boolean isOperational() {
        return launchStatus == CountryLaunchStatus.LIVE || launchStatus == CountryLaunchStatus.BETA;
    }

    public boolean canAcceptNewUsers() {
        return isOperational() && registrationEnabled;
    }

    public void updateLaunchStatus(CountryLaunchStatus newStatus) {
        this.launchStatus = newStatus;
    }

    public void enableFeature(String featureName, boolean enabled) {
        switch (featureName.toLowerCase()) {
            case "registration" -> this.registrationEnabled = enabled;
            case "contentpublishing" -> this.contentPublishingEnabled = enabled;
            case "partneronboarding" -> this.partnerOnboardingEnabled = enabled;
            case "payments" -> this.paymentsEnabled = enabled;
            case "booking" -> this.bookingEnabled = enabled;
            case "ticketing" -> this.ticketingEnabled = enabled;
            case "artisancommerce" -> this.artisanCommerceEnabled = enabled;
            case "culturemodule" -> this.cultureModuleEnabled = enabled;
            default -> throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOfficialName() {
        return officialName;
    }

    public void setOfficialName(String officialName) {
        this.officialName = officialName;
    }

    public String getContinentCode() {
        return continentCode;
    }

    public void setContinentCode(String continentCode) {
        this.continentCode = continentCode;
    }

    public String getDefaultLanguageCode() {
        return defaultLanguageCode;
    }

    public void setDefaultLanguageCode(String defaultLanguageCode) {
        this.defaultLanguageCode = defaultLanguageCode;
    }

    public String getDefaultCurrencyCode() {
        return defaultCurrencyCode;
    }

    public void setDefaultCurrencyCode(String defaultCurrencyCode) {
        this.defaultCurrencyCode = defaultCurrencyCode;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    public CountryLaunchStatus getLaunchStatus() {
        return launchStatus;
    }

    public void setLaunchStatus(CountryLaunchStatus launchStatus) {
        this.launchStatus = launchStatus;
    }

    public Boolean getRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(Boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public Boolean getContentPublishingEnabled() {
        return contentPublishingEnabled;
    }

    public void setContentPublishingEnabled(Boolean contentPublishingEnabled) {
        this.contentPublishingEnabled = contentPublishingEnabled;
    }

    public Boolean getPartnerOnboardingEnabled() {
        return partnerOnboardingEnabled;
    }

    public void setPartnerOnboardingEnabled(Boolean partnerOnboardingEnabled) {
        this.partnerOnboardingEnabled = partnerOnboardingEnabled;
    }

    public Boolean getPaymentsEnabled() {
        return paymentsEnabled;
    }

    public void setPaymentsEnabled(Boolean paymentsEnabled) {
        this.paymentsEnabled = paymentsEnabled;
    }

    public Boolean getBookingEnabled() {
        return bookingEnabled;
    }

    public void setBookingEnabled(Boolean bookingEnabled) {
        this.bookingEnabled = bookingEnabled;
    }

    public Boolean getTicketingEnabled() {
        return ticketingEnabled;
    }

    public void setTicketingEnabled(Boolean ticketingEnabled) {
        this.ticketingEnabled = ticketingEnabled;
    }

    public Boolean getArtisanCommerceEnabled() {
        return artisanCommerceEnabled;
    }

    public void setArtisanCommerceEnabled(Boolean artisanCommerceEnabled) {
        this.artisanCommerceEnabled = artisanCommerceEnabled;
    }

    public Boolean getCultureModuleEnabled() {
        return cultureModuleEnabled;
    }

    public void setCultureModuleEnabled(Boolean cultureModuleEnabled) {
        this.cultureModuleEnabled = cultureModuleEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
