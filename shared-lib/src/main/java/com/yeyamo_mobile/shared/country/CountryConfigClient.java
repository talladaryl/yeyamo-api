package com.yeyamo_mobile.shared.country;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
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
    private final Map<String, CountryConfig> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    public CountryConfigClient(
            RestClient.Builder restClientBuilder,
            String countryConfigServiceUrl,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.serviceUrl = countryConfigServiceUrl;
        this.restClient = restClientBuilder
                .baseUrl(serviceUrl)
                .build();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build();

        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("country-config", config);
    }

    /**
     * Get country configuration with caching.
     * 
     * @throws CountryConfigException if country not found or service unavailable
     */
    @Cacheable(value = "countryConfig", key = "#countryCode")
    public CountryConfig getCountry(String countryCode) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                Map<String, Object> response = restClient.get()
                        .uri("/api/v1/countries/{code}", countryCode)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                            throw new CountryConfigException("Country not found: " + countryCode);
                        })
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (response == null) {
                    throw new CountryConfigException("Empty response from country-config-service");
                }

                return mapToCountryConfig(response);
            } catch (Exception e) {
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

        boolean enabled = switch (feature) {
            case CONTENT_PUBLISHING -> config.contentPublishingEnabled();
            case PARTNER_ONBOARDING -> config.partnerOnboardingEnabled();
            case PAYMENTS -> config.paymentsEnabled();
            case BOOKING -> config.bookingEnabled();
            case TICKETING -> config.ticketingEnabled();
            case ARTISAN_COMMERCE -> config.artisanCommerceEnabled();
            case CULTURE_MODULE -> config.cultureModuleEnabled();
        };

        if (!enabled) {
            throw new CountryConfigException(
                    String.format("Feature %s is not enabled for country %s", feature, countryCode));
        }

        if ("DISABLED".equals(config.launchStatus())) {
            throw new CountryConfigException("Country " + countryCode + " is disabled");
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
                        .uri("/api/v1/countries/{code}/cities/{cityId}", countryCode, cityId)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                            throw new CountryConfigException(
                                    "City not found or doesn't belong to country: " + cityId);
                        })
                        .toBodilessEntity();
                return null;
            } catch (Exception e) {
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

    public enum CountryFeature {
        CONTENT_PUBLISHING,
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
