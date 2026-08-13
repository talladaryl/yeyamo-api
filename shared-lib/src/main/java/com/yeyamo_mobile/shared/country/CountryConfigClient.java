package com.yeyamo_mobile.shared.country;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Resilient client for country-config-service.
 * 
 * Features:
 * - Local short cache (5 minutes)
 * - Circuit breaker
 * - Timeout (2 seconds)
 * - Strict failure handling (no silent fallback)
 */
@Component
public class CountryConfigClient {

    private final RestClient restClient;
    private final String serviceUrl;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, CachedCountryConfig> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    public CountryConfigClient(
            RestClient.Builder restClientBuilder,
            @Value("${yeyamo.services.country-config.url:http://country-config-service}") String countryConfigServiceUrl) {
        this.serviceUrl = countryConfigServiceUrl.replaceAll("/$", "");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT);
        this.restClient = restClientBuilder
                .baseUrl(serviceUrl)
                .requestFactory(requestFactory)
                .build();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build();

        this.circuitBreaker = CircuitBreakerRegistry.of(config).circuitBreaker("country-config");
    }

    /**
     * Get country configuration with caching.
     * 
     * @throws CountryConfigException if country not found or service unavailable
     */
    public CountryConfig getCountry(String countryCode) {
        String normalizedCode = normalizeCountryCode(countryCode);
        CachedCountryConfig cached = cache.get(normalizedCode);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }
        return circuitBreaker.executeSupplier(() -> {
            try {
                Map<String, Object> response = restClient.get()
                        .uri("/api/v1/countries/{code}", normalizedCode)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                            throw new CountryConfigException("Country not found: " + normalizedCode);
                        })
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (response == null) {
                    throw new CountryConfigException("Empty response from country-config-service");
                }

                CountryConfig config = mapToCountryConfig(response);
                cache.put(normalizedCode, new CachedCountryConfig(config, Instant.now().plus(CACHE_TTL)));
                return config;
            } catch (Exception e) {
                if (e instanceof CountryConfigException countryConfigException) {
                    throw countryConfigException;
                }
                throw new CountryConfigException("Failed to fetch country config: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Validate if a feature is enabled for a country.
     * 
     * @throws CountryConfigException if feature is disabled or service unavailable
     */
    public void validateFeature(String countryCode, CountryFeature feature) {
        CountryConfig config = getCountry(countryCode);

        requireOperational(config);
        boolean enabled = isFeatureEnabled(config, feature);

        if (!enabled) {
            throw new CountryConfigException(
                    String.format("Feature %s is not enabled for country %s", feature, countryCode));
        }
    }

    /** Validates that at least one requested feature is enabled. */
    public void validateAnyFeature(String countryCode, Collection<CountryFeature> features) {
        if (features == null || features.isEmpty()) {
            throw new IllegalArgumentException("At least one country feature is required");
        }
        CountryConfig config = getCountry(countryCode);
        requireOperational(config);
        if (features.stream().noneMatch(feature -> isFeatureEnabled(config, feature))) {
            throw new CountryConfigException("None of the required features are enabled for country " + countryCode);
        }
    }

    private boolean isFeatureEnabled(CountryConfig config, CountryFeature feature) {
        return switch (feature) {
            case CONTENT_PUBLISHING -> config.contentPublishingEnabled();
            case PLACE_PUBLISHING -> config.placePublishingEnabled();
            case EVENT_FEATURE -> config.eventFeatureEnabled();
            case PARTNER_ONBOARDING -> config.partnerOnboardingEnabled();
            case PAYMENTS -> config.paymentsEnabled();
            case BOOKING -> config.bookingEnabled();
            case TICKETING -> config.ticketingEnabled();
            case ARTISAN_COMMERCE -> config.artisanCommerceEnabled();
            case CULTURE_MODULE -> config.cultureModuleEnabled();
        };
    }

    private void requireOperational(CountryConfig config) {
        if (!"LIVE".equals(config.launchStatus()) && !"BETA".equals(config.launchStatus())) {
            throw new CountryConfigException("Country " + config.code() + " is not operational");
        }
    }

    /**
     * Validate city belongs to country.
     * 
     * @throws CountryConfigException if city not found or doesn't belong to country
     */
    public void validateCity(String countryCode, UUID cityId) {
        if (cityId == null) {
            return; // City is optional
        }

        circuitBreaker.executeSupplier(() -> {
            try {
                restClient.get()
                        .uri("/api/v1/countries/{code}/cities/{cityId}", normalizeCountryCode(countryCode), cityId)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                            throw new CountryConfigException(
                                    "City not found or doesn't belong to country: " + cityId);
                        })
                        .toBodilessEntity();
                return null;
            } catch (Exception e) {
                if (e instanceof CountryConfigException countryConfigException) {
                    throw countryConfigException;
                }
                throw new CountryConfigException("Failed to validate city: " + e.getMessage(), e);
            }
        });
    }

    private CountryConfig mapToCountryConfig(Map<String, Object> response) {
        return new CountryConfig(
                (String) response.get("code"),
                (String) response.get("name"),
                (String) response.get("launchStatus"),
                (Boolean) response.getOrDefault("registrationEnabled", false),
                (Boolean) response.getOrDefault("contentPublishingEnabled", false),
                (Boolean) response.getOrDefault("placePublishingEnabled", false),
                (Boolean) response.getOrDefault("eventFeatureEnabled", false),
                (Boolean) response.getOrDefault("partnerOnboardingEnabled", false),
                (Boolean) response.getOrDefault("paymentsEnabled", false),
                (Boolean) response.getOrDefault("bookingEnabled", false),
                (Boolean) response.getOrDefault("ticketingEnabled", false),
                (Boolean) response.getOrDefault("artisanCommerceEnabled", false),
                (Boolean) response.getOrDefault("cultureModuleEnabled", false),
                (String) response.get("defaultLanguageCode"),
                (String) response.get("defaultTimezone"),
                (String) response.get("defaultCurrencyCode")
        );
    }

    public record CountryConfig(
            String code,
            String name,
            String launchStatus,
            boolean registrationEnabled,
            boolean contentPublishingEnabled,
            boolean placePublishingEnabled,
            boolean eventFeatureEnabled,
            boolean partnerOnboardingEnabled,
            boolean paymentsEnabled,
            boolean bookingEnabled,
            boolean ticketingEnabled,
            boolean artisanCommerceEnabled,
            boolean cultureModuleEnabled,
            String defaultLanguageCode,
            String defaultTimezone,
            String defaultCurrencyCode
    ) {}

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || !countryCode.trim().matches("[A-Za-z]{2}")) {
            throw new CountryConfigException("Country code must be an ISO 3166-1 alpha-2 code");
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private record CachedCountryConfig(CountryConfig value, Instant expiresAt) {}

    public enum CountryFeature {
        CONTENT_PUBLISHING,
        PLACE_PUBLISHING,
        EVENT_FEATURE,
        PARTNER_ONBOARDING,
        PAYMENTS,
        BOOKING,
        TICKETING,
        ARTISAN_COMMERCE,
        CULTURE_MODULE
    }

    public static class CountryConfigException extends RuntimeException {
        public CountryConfigException(String message) {
            super(message);
        }

        public CountryConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
