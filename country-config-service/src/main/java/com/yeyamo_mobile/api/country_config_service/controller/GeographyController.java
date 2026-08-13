package com.yeyamo_mobile.api.country_config_service.controller;

import com.yeyamo_mobile.api.country_config_service.dto.*;
import com.yeyamo_mobile.api.country_config_service.service.GeographyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Geography API - Generic administrative structure support.
 * 
 * NOT tied to Cameroon-specific concepts.
 * Uses level-based hierarchy with country-specific labels.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Geography", description = "Generic administrative structure API")
public class GeographyController {

    private final GeographyService geographyService;

    public GeographyController(GeographyService geographyService) {
        this.geographyService = geographyService;
    }

    // ===========================
    // Administrative Areas
    // ===========================

    @GetMapping("/countries/{code}/administrative-areas")
    @Operation(summary = "Get administrative areas", 
               description = "Returns all administrative areas for a country (regions, states, departments, etc.)")
    public ResponseEntity<List<AdministrativeAreaDto>> getAdministrativeAreas(@PathVariable String code) {
        return ResponseEntity.ok(geographyService.getAdministrativeAreas(code));
    }

    @GetMapping("/countries/{code}/administrative-areas/paged")
    @Operation(summary = "Get administrative areas (paginated)")
    public ResponseEntity<Page<AdministrativeAreaDto>> getAdministrativeAreasPaged(
            @PathVariable String code,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(geographyService.getAdministrativeAreas(code, pageable));
    }

    @GetMapping("/countries/{code}/administrative-areas/top-level")
    @Operation(summary = "Get top-level administrative areas", 
               description = "Returns level 1 areas only (e.g., regions in Cameroon, states in Nigeria)")
    public ResponseEntity<List<AdministrativeAreaDto>> getTopLevelAreas(@PathVariable String code) {
        return ResponseEntity.ok(geographyService.getTopLevelAreas(code));
    }

    @GetMapping("/administrative-areas/{id}")
    @Operation(summary = "Get administrative area by ID")
    public ResponseEntity<AdministrativeAreaDto> getAdministrativeArea(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getAdministrativeArea(id));
    }

    @GetMapping("/administrative-areas/{id}/children")
    @Operation(summary = "Get children of administrative area", 
               description = "Returns sub-divisions (e.g., departments within a region)")
    public ResponseEntity<List<AdministrativeAreaDto>> getChildren(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getChildren(id));
    }

    @GetMapping("/countries/{code}/administrative-labels")
    @Operation(summary = "Get administrative level labels", 
               description = "Returns country-specific terminology (e.g., Cameroon: Région/Département, Nigeria: State/LGA)")
    public ResponseEntity<List<AdministrativeLevelLabelDto>> getLevelLabels(@PathVariable String code) {
        return ResponseEntity.ok(geographyService.getLevelLabels(code));
    }

    // ===========================
    // Cities
    // ===========================

    @GetMapping("/countries/{code}/cities")
    @Operation(summary = "Get cities", description = "Returns all cities in a country")
    public ResponseEntity<List<CityDto>> getCities(@PathVariable String code) {
        return ResponseEntity.ok(geographyService.getCities(code));
    }

    @GetMapping("/countries/{code}/cities/paged")
    @Operation(summary = "Get cities (paginated)")
    public ResponseEntity<Page<CityDto>> getCitiesPaged(
            @PathVariable String code,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(geographyService.getCities(code, pageable));
    }

    @GetMapping("/cities/{id}")
    @Operation(summary = "Get city by ID")
    public ResponseEntity<CityDto> getCity(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getCity(id));
    }

    // ===========================
    // Localities
    // ===========================

    @GetMapping("/cities/{id}/localities")
    @Operation(summary = "Get localities", 
               description = "Returns localities within a city (neighborhoods, quartiers, wards, etc.)")
    public ResponseEntity<List<LocalityDto>> getLocalitiesByCity(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getLocalitiesByCity(id));
    }

    @GetMapping("/cities/{id}/localities/paged")
    @Operation(summary = "Get localities (paginated)")
    public ResponseEntity<Page<LocalityDto>> getLocalitiesByCityPaged(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(geographyService.getLocalitiesByCity(id, pageable));
    }

    @GetMapping("/localities/{id}")
    @Operation(summary = "Get locality by ID")
    public ResponseEntity<LocalityDto> getLocality(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getLocality(id));
    }
}
