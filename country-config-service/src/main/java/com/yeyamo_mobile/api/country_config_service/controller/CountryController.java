package com.yeyamo_mobile.api.country_config_service.controller;

import com.yeyamo_mobile.api.country_config_service.dto.*;
import com.yeyamo_mobile.api.country_config_service.service.CountryConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public API for country configuration.
 * No authentication required for read operations.
 */
@RestController
@RequestMapping("/api/v1/countries")
@Tag(name = "Country Configuration", description = "Public country configuration API")
public class CountryController {

    private final CountryConfigService countryConfigService;

    public CountryController(CountryConfigService countryConfigService) {
        this.countryConfigService = countryConfigService;
    }

    @GetMapping
    @Operation(summary = "Get all countries", description = "Returns all countries regardless of launch status")
    public ResponseEntity<List<CountryDto>> getAllCountries() {
        return ResponseEntity.ok(countryConfigService.getAllCountries());
    }

    @GetMapping("/available")
    @Operation(summary = "Get available countries", description = "Returns only LIVE and BETA countries")
    public ResponseEntity<List<CountryDto>> getAvailableCountries() {
        return ResponseEntity.ok(countryConfigService.getAvailableCountries());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get country by code", description = "Returns country details by ISO 3166-1 alpha-2 code")
    public ResponseEntity<CountryDto> getCountryByCode(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryByCode(code));
    }

    @GetMapping("/{code}/configuration")
    @Operation(summary = "Get full country configuration", 
               description = "Returns country with languages, currencies, and timezones")
    public ResponseEntity<CountryConfigurationDto> getCountryConfiguration(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryConfiguration(code));
    }

    @GetMapping("/{code}/features")
    @Operation(summary = "Get country features", description = "Returns feature flags for a country")
    public ResponseEntity<FeatureFlagsDto> getCountryFeatures(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryFeatures(code));
    }

    @GetMapping("/{code}/languages")
    @Operation(summary = "Get country languages", description = "Returns all languages supported in a country")
    public ResponseEntity<List<LanguageDto>> getCountryLanguages(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryLanguages(code));
    }

    @GetMapping("/{code}/currencies")
    @Operation(summary = "Get country currencies", description = "Returns all currencies accepted in a country")
    public ResponseEntity<List<CurrencyDto>> getCountryCurrencies(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryCurrencies(code));
    }

    @GetMapping("/{code}/timezones")
    @Operation(summary = "Get country timezones", description = "Returns all timezones for a country")
    public ResponseEntity<List<TimezoneDto>> getCountryTimezones(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryTimezones(code));
    }
}
