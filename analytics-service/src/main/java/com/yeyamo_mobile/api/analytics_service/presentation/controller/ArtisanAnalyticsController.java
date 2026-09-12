package com.yeyamo_mobile.api.analytics_service.presentation.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.analytics_service.application.service.CultureAnalyticsService;
import com.yeyamo_mobile.api.analytics_service.infrastructure.client.PartnerIdentityClient;

@RestController
@RequestMapping("/api/v1/analytics/artisans")
public class ArtisanAnalyticsController {
    private final CultureAnalyticsService analytics;
    private final PartnerIdentityClient partnerIdentity;
    public ArtisanAnalyticsController(CultureAnalyticsService analytics, PartnerIdentityClient partnerIdentity) {
        this.analytics = analytics;
        this.partnerIdentity = partnerIdentity;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Map<String, Object>> mine(@RequestParam(defaultValue = "30") int periodDays,
            JwtAuthenticationToken authentication) {
        String artisanId = partnerIdentity.currentPartnerId(authentication.getToken().getTokenValue());
        return ResponseEntity.ok(analytics.getArtisanAnalyticsForPeriod(artisanId, periodDays));
    }
}
