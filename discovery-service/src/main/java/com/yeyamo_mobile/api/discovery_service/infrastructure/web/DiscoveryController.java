package com.yeyamo_mobile.api.discovery_service.infrastructure.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.*;
import com.yeyamo_mobile.api.discovery_service.application.*;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {

    private final DiscoveryQueryService queries;

    public DiscoveryController(DiscoveryQueryService queries) { this.queries = queries; }

    /**
     * Full-text search with all filters.
     * Culture & Artisan params: languageCode, cultureType, materialId, techniqueId,
     * availability, verified, countryCode, adminLevel1Id, cityId.
     */
    @GetMapping("/search")
    @Operation(summary = "Search discoverable places, content, artworks and culture",
               security = @SecurityRequirement(name = "bearerAuth"))
    public DiscoveryPage search(
            @RequestParam(required = false, name = "q")             String query,
            @RequestParam(required = false)                         DiscoveryType type,
            @RequestParam(required = false)                         String categoryCode,
            @RequestParam(required = false)                         String regionCode,
            // geo
            @RequestParam(required = false, name = "lat")           Double latitude,
            @RequestParam(required = false, name = "lng")           Double longitude,
            @RequestParam(required = false)                         Double radiusKm,
            // culture & artisan
            @RequestParam(required = false)                         String countryCode,
            @RequestParam(required = false)                         List<String> countries,
            @RequestParam(required = false)                         String adminLevel1Id,
            @RequestParam(required = false)                         String cityId,
            @RequestParam(required = false)                         String languageCode,
            @RequestParam(required = false)                         String cultureType,
            @RequestParam(required = false)                         String materialId,
            @RequestParam(required = false)                         String techniqueId,
            @RequestParam(required = false)                         Boolean availability,
            @RequestParam(required = false)                         Boolean verified,
            @RequestParam(required = false, defaultValue = "COUNTRY") DiscoveryScope scope,
            // pagination
            @RequestParam(defaultValue = "0")                       int page,
            @RequestParam(defaultValue = "20")                      int size,
            Authentication auth) {

        return queries.search(new DiscoverySearch(
                query, type, categoryCode, regionCode,
                latitude, longitude, radiusKm,
                page, size, false,
                resolveCountry(countryCode, countries, scope, auth), adminLevel1Id, cityId,
                languageCode, cultureType, materialId, techniqueId,
                availability, verified, normalizeCountries(countries), scope));
    }

    @GetMapping("/trending")
    @Operation(summary = "List trending places and content",
               security = @SecurityRequirement(name = "bearerAuth"))
    public DiscoveryPage trending(
            @RequestParam(required = false)     DiscoveryType type,
            @RequestParam(required = false)     String regionCode,
            @RequestParam(required = false)     String countryCode,
            @RequestParam(required = false, defaultValue = "COUNTRY") DiscoveryScope scope,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            Authentication auth) {
        return queries.search(new DiscoverySearch(
                null, type, null, regionCode,
                null, null, null,
                page, size, true,
                resolveCountry(countryCode, null, scope, auth), null, null, null, null, null, null, null, null,
                Set.of(), scope));
    }

    private String resolveCountry(String country, List<String> countries, DiscoveryScope scope, Authentication auth) {
        if (country != null && !country.isBlank()) return country;
        if (countries != null && !countries.isEmpty()) return null;
        if (scope == DiscoveryScope.AFRICA || !(auth instanceof JwtAuthenticationToken jwt)) return null;
        return jwt.getToken().getClaimAsString("countryCode");
    }
    private Set<String> normalizeCountries(List<String> values) {
        if (values == null) return Set.of(); Set<String> result = new LinkedHashSet<>();
        values.forEach(value -> { if (value != null) for (String code : value.split(",")) if (code.trim().matches("[A-Za-z]{2}")) result.add(code.trim().toUpperCase(Locale.ROOT)); });
        return result;
    }
}
