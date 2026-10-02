package com.yeyamo_mobile.api.user_service.interfaces.rest;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.user_service.application.UserProfileService;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.MyProfileResponse;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.PublicProfileResponse;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UpdateDiscoveryPreferencesRequest;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UpdateLanguageRequest;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UpdateLocationRequest;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UpdatePreferencesRequest;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UpdateProfileRequest;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UpdateInterestsRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) { this.service = service; }

    @GetMapping("/me")
    public MyProfileResponse me(JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String displayName = authentication.getToken().getClaimAsString("email");
        // Auth-service signs the country chosen during registration into the
        // access token.  Materialise it here so the first authenticated call
        // cannot race the asynchronous user.created Kafka consumer and leave
        // Create without a country.
        String countryCode = authentication.getToken().getClaimAsString("country");
        return MyProfileResponse.from(service.getOrCreateWithInitialLocation(
                authentication.getName(), displayName, countryCode, null, null, null, correlationId));
    }

    @PutMapping("/me")
    public MyProfileResponse update(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return MyProfileResponse.from(service.update(authentication.getName(), request.displayName(),
                request.avatarUrl(), request.bio(), request.language(), request.visibility(), correlationId));
    }

    @PatchMapping("/me/preferences")
    public MyProfileResponse preferences(@Valid @RequestBody UpdatePreferencesRequest request, Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return MyProfileResponse.from(service.updatePreferences(authentication.getName(),
                request.notificationsEnabled(), request.locationSharingEnabled(), request.preferredRegionId(),
                correlationId));
    }

    @PutMapping("/me/interests")
    public MyProfileResponse interests(@Valid @RequestBody UpdateInterestsRequest request, Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return MyProfileResponse.from(service.updateInterests(authentication.getName(), request.categoryCodes(),
                request.completeOnboarding(), correlationId));
    }

    @PostMapping("/me/activity")
    public MyProfileResponse activity(@RequestParam(defaultValue = "false") boolean login, Authentication authentication) {
        return MyProfileResponse.from(service.recordActivity(authentication.getName(), login));
    }

    /**
     * Update user location (country, city, administrative areas, timezone).
     * Endpoint: PATCH /api/v1/users/me/location
     */
    @PatchMapping("/me/location")
    public MyProfileResponse updateLocation(@Valid @RequestBody UpdateLocationRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return MyProfileResponse.from(service.updateLocation(authentication.getName(),
                request.countryCode(), request.adminLevel1Id(), request.adminLevel2Id(),
                request.cityId(), request.localityId(), request.timezone(), correlationId));
    }

    /**
     * Update user language preferences (preferred language and content languages).
     * Endpoint: PATCH /api/v1/users/me/language
     */
    @PatchMapping("/me/language")
    public MyProfileResponse updateLanguage(@Valid @RequestBody UpdateLanguageRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return MyProfileResponse.from(service.updateLanguagePreferences(authentication.getName(),
                request.preferredLanguageCode(), request.contentLanguages(), correlationId));
    }

    /**
     * Update discovery preferences (content countries, local radius, African content, currency).
     * Endpoint: PATCH /api/v1/users/me/discovery-preferences
     */
    @PatchMapping("/me/discovery-preferences")
    public MyProfileResponse updateDiscoveryPreferences(@Valid @RequestBody UpdateDiscoveryPreferencesRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return MyProfileResponse.from(service.updateDiscoveryPreferences(authentication.getName(),
                request.contentCountries(), request.localRadiusKm(), request.discoverAfricanContent(),
                request.preferredCurrencyCode(), correlationId));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        service.delete(authentication.getName(), correlationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public PublicProfileResponse byId(@PathVariable UUID id, Authentication authentication) {
        return PublicProfileResponse.from(service.publicProfile(id, authentication == null ? null : authentication.getName()));
    }

    @GetMapping
    public Page<PublicProfileResponse> search(@RequestParam(defaultValue = "") String q, Pageable pageable) {
        return service.search(q, pageable).map(PublicProfileResponse::from);
    }
}
