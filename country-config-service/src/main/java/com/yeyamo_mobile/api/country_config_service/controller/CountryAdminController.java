package com.yeyamo_mobile.api.country_config_service.controller;

import com.yeyamo_mobile.api.country_config_service.dto.*;
import com.yeyamo_mobile.api.country_config_service.service.CountryConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin API for country configuration management.
 * Requires SUPER_ADMIN role for launch status changes.
 */
@RestController
@RequestMapping("/api/v1/admin/countries")
@Tag(name = "Country Administration", description = "Admin API for country configuration management")
@SecurityRequirement(name = "bearer-jwt")
public class CountryAdminController {

    private final CountryConfigService countryConfigService;

    public CountryAdminController(CountryConfigService countryConfigService) {
        this.countryConfigService = countryConfigService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all countries (admin)", description = "Admin view of all countries")
    public ResponseEntity<List<CountryDto>> getAllCountries() {
        return ResponseEntity.ok(countryConfigService.getAllCountries());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get country by code (admin)", description = "Admin view of country details")
    public ResponseEntity<CountryDto> getCountryByCode(@PathVariable String code) {
        return ResponseEntity.ok(countryConfigService.getCountryByCode(code));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update country", description = "Update country configuration (excludes launch status and features)")
    public ResponseEntity<CountryDto> updateCountry(
            @PathVariable String code,
            @Valid @RequestBody UpdateCountryRequest request
    ) {
        return ResponseEntity.ok(countryConfigService.updateCountry(code, request));
    }

    @PatchMapping("/{code}/launch-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update launch status", 
               description = "Update country launch status. SUPER_ADMIN only. Critical operation.")
    public ResponseEntity<CountryDto> updateLaunchStatus(
            @PathVariable String code,
            @Valid @RequestBody UpdateLaunchStatusRequest request
    ) {
        return ResponseEntity.ok(countryConfigService.updateLaunchStatus(code, request.launchStatus()));
    }

    @PatchMapping("/{code}/features")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update features", description = "Update country feature flags")
    public ResponseEntity<CountryDto> updateFeatures(
            @PathVariable String code,
            @Valid @RequestBody UpdateFeaturesRequest request
    ) {
        return ResponseEntity.ok(countryConfigService.updateFeatures(code, request));
    }
}
