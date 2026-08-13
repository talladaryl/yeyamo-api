package com.yeyamo_mobile.api.country_config_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Currencies accepted in a country.
 * A country can support multiple currencies (e.g., XAF + USD for international).
 */
@Entity
@Table(name = "country_currencies", indexes = {
    @Index(name = "idx_country_curr_country_id", columnList = "countryId"),
    @Index(name = "idx_country_curr_code", columnList = "currencyCode")
})
public class CountryCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID countryId;

    /**
     * Currency code (ISO 4217, e.g., "XAF", "USD")
     */
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters (ISO 4217)")
    @Column(nullable = false, length = 3)
    private String currencyCode;

    /**
     * Currency name (e.g., "Central African CFA franc")
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Currency symbol (e.g., "FCFA", "$")
     */
    @Size(max = 10)
    @Column(length = 10)
    private String symbol;

    /**
     * Number of decimal places (e.g., 0 for XAF, 2 for USD)
     */
    @NotNull
    @Column(nullable = false)
    private Integer decimalPlaces = 2;

    /**
     * Is this the default currency for the country?
     */
    @NotNull
    @Column(nullable = false)
    private Boolean isDefault = false;

    // Constructors
    protected CountryCurrency() {
    }

    public CountryCurrency(UUID countryId, String currencyCode, String name, String symbol, Integer decimalPlaces, Boolean isDefault) {
        this.countryId = countryId;
        this.currencyCode = currencyCode;
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
