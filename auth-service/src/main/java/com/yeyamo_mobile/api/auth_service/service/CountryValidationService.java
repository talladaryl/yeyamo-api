package com.yeyamo_mobile.api.auth_service.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

/**
 * Service to validate country and geographic data against country-config-service.
 */
@Service
public class CountryValidationService {

    private final RestClient restClient;
    private final String countryServiceUrl;

    public CountryValidationService(
            RestClient.Builder restClientBuilder,
            @Value("${yeyamo.services.country-config.url}") String countryServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.countryServiceUrl = countryServiceUrl;
    }

    /**
     * Validates a country code and returns country details.
     * 
     * @param countryCode ISO 3166-1 alpha-2 code
     * @return CountryValidationResult with country details
     * @throws ApiException if country not found or invalid
     */
    public CountryValidationResult validateCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new ApiException("COUNTRY_REQUIRED", "Code pays requis", HttpStatus.BAD_REQUEST);
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(countryServiceUrl + "/api/v1/countries/" + countryCode)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                        throw new ApiException("COUNTRY_NOT_FOUND", 
                                "Pays introuvable: " + countryCode, HttpStatus.BAD_REQUEST);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                throw new ApiException("COUNTRY_SERVICE_ERROR", 
                        "Erreur lors de la validation du pays", HttpStatus.SERVICE_UNAVAILABLE);
            }

            String launchStatus = (String) response.get("launchStatus");
            Boolean registrationEnabled = (Boolean) response.get("registrationEnabled");
            String defaultLanguage = (String) response.get("defaultLanguageCode");
            String defaultTimezone = (String) response.get("defaultTimezone");
            String defaultCurrency = (String) response.get("defaultCurrencyCode");

            return new CountryValidationResult(
                    countryCode,
                    launchStatus,
                    registrationEnabled != null && registrationEnabled,
                    defaultLanguage,
                    defaultTimezone,
                    defaultCurrency
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("COUNTRY_SERVICE_ERROR", 
                    "Erreur lors de la validation du pays", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Validates a city belongs to the specified country.
     * 
     * @param countryCode ISO 3166-1 alpha-2 code
     * @param cityId City UUID
     * @throws ApiException if city not found or doesn't belong to country
     */
    public void validateCity(String countryCode, UUID cityId) {
        if (cityId == null) {
            return; // City is optional
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(countryServiceUrl + "/api/v1/countries/" + countryCode + "/cities/" + cityId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                        throw new ApiException("CITY_NOT_FOUND_OR_INVALID", 
                                "Ville introuvable ou ne correspond pas au pays", HttpStatus.BAD_REQUEST);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                throw new ApiException("CITY_NOT_FOUND_OR_INVALID", 
                        "Ville introuvable ou ne correspond pas au pays", HttpStatus.BAD_REQUEST);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("COUNTRY_SERVICE_ERROR", 
                    "Erreur lors de la validation de la ville", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Validates country registration eligibility and enforces rules.
     * 
     * @param countryCode ISO 3166-1 alpha-2 code
     * @param allowComingSoon Whether to allow registration for COMING_SOON countries
     * @return CountryValidationResult with country details
     * @throws ApiException if registration not allowed
     */
    public CountryValidationResult validateRegistrationEligibility(
            String countryCode, 
            boolean allowComingSoon) {
        
        CountryValidationResult country = validateCountry(countryCode);

        // Check if country is disabled
        if ("DISABLED".equals(country.launchStatus())) {
            throw new ApiException("COUNTRY_DISABLED", 
                    "Ce pays n'est pas disponible", HttpStatus.FORBIDDEN);
        }

        // Check if registration is enabled
        if (!country.registrationEnabled()) {
            throw new ApiException("REGISTRATION_DISABLED", 
                    "Les inscriptions ne sont pas ouvertes pour ce pays", HttpStatus.FORBIDDEN);
        }

        // Check COMING_SOON status
        if ("COMING_SOON".equals(country.launchStatus()) && !allowComingSoon) {
            throw new ApiException("COUNTRY_COMING_SOON", 
                    "Ce pays sera bientôt disponible", HttpStatus.FORBIDDEN);
        }

        return country;
    }

    public record CountryValidationResult(
            String countryCode,
            String launchStatus,
            boolean registrationEnabled,
            String defaultLanguageCode,
            String defaultTimezone,
            String defaultCurrencyCode
    ) {}
}
