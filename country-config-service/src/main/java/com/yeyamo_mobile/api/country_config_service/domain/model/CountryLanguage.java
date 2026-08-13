package com.yeyamo_mobile.api.country_config_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Languages supported in a country.
 * A country can have multiple languages (e.g., Cameroon: French, English).
 */
@Entity
@Table(name = "country_languages", indexes = {
    @Index(name = "idx_country_lang_country_id", columnList = "countryId"),
    @Index(name = "idx_country_lang_code", columnList = "languageCode")
})
public class CountryLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID countryId;

    /**
     * Language code (ISO 639-1 or BCP 47, e.g., "fr", "en", "fr-CM")
     */
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String languageCode;

    /**
     * Language name (e.g., "French", "English")
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Is this the default language for the country?
     */
    @NotNull
    @Column(nullable = false)
    private Boolean isDefault = false;

    /**
     * Display order in language selection lists
     */
    @Column(nullable = false)
    private Integer displayOrder = 0;

    // Constructors
    protected CountryLanguage() {
    }

    public CountryLanguage(UUID countryId, String languageCode, String name, Boolean isDefault, Integer displayOrder) {
        this.countryId = countryId;
        this.languageCode = languageCode;
        this.name = name;
        this.isDefault = isDefault;
        this.displayOrder = displayOrder;
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

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
