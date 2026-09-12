package com.yeyamo_mobile.api.analytics_service.presentation.controller;

import com.yeyamo_mobile.api.analytics_service.application.dto.CultureOverviewResponse;
import com.yeyamo_mobile.api.analytics_service.application.service.CultureAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.Map;

/**
 * Analytics endpoints for Culture and Artisan features
 */
@RestController
@RequestMapping("/api/v1/analytics/culture")
@RequiredArgsConstructor
@Tag(name = "Culture Analytics", description = "Analytics for cultural content and artisan features")
@SecurityRequirement(name = "bearer-jwt")
public class CultureAnalyticsController {
    
    private final CultureAnalyticsService analyticsService;
    
    /**
     * Get overall culture platform overview
     */
    @GetMapping("/overview")
    @Operation(summary = "Get culture platform overview")
    public ResponseEntity<CultureOverviewResponse> getCultureOverview() {
        CultureOverviewResponse overview = analyticsService.getCultureOverview();
        return ResponseEntity.ok(overview);
    }
    
    /**
     * Get language-specific analytics
     */
    @GetMapping("/languages/{languageCode}")
    @Operation(summary = "Get language analytics")
    public ResponseEntity<Map<String, Object>> getLanguageAnalytics(
            @PathVariable String languageCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = analyticsService.getLanguageAnalytics(
                languageCode, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get country-specific culture analytics
     */
    @GetMapping("/countries/{countryCode}")
    @Operation(summary = "Get country culture analytics")
    public ResponseEntity<Map<String, Object>> getCountryAnalytics(
            @PathVariable String countryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = analyticsService.getCountryAnalytics(
                countryCode, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get artisan performance metrics
     */
    @GetMapping("/artisans/{artisanId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get artisan analytics")
    public ResponseEntity<Map<String, Object>> getArtisanAnalytics(
            @PathVariable String artisanId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = analyticsService.getArtisanAnalytics(
                artisanId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get artwork performance metrics
     */
    @GetMapping("/artworks/{artworkId}")
    @Operation(summary = "Get artwork analytics")
    public ResponseEntity<Map<String, Object>> getArtworkAnalytics(
            @PathVariable String artworkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = analyticsService.getArtworkAnalytics(
                artworkId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get contribution statistics
     */
    @GetMapping("/contributions")
    @Operation(summary = "Get contribution analytics")
    public ResponseEntity<Map<String, Object>> getContributionAnalytics(
            @RequestParam(required = false) String contributionType,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = analyticsService.getContributionAnalytics(
                contributionType, countryCode, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get trending content
     */
    @GetMapping("/trending")
    @Operation(summary = "Get trending cultural content")
    public ResponseEntity<Map<String, Object>> getTrendingContent(
            @RequestParam(required = false) String countryCode,
            @RequestParam(defaultValue = "7") @Parameter(description = "Days to look back") int days,
            @RequestParam(defaultValue = "10") @Parameter(description = "Number of items") int limit) {
        
        Map<String, Object> trending = analyticsService.getTrendingContent(
                countryCode, days, limit);
        return ResponseEntity.ok(trending);
    }
}
