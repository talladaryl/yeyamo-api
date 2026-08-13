package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.service.CountryValidationService.CountryValidationResult;

class CountryValidationServiceTests {

    private CountryValidationService service;
    private RestClient restClient;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setup() {
        restClient = mock(RestClient.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.build()).thenReturn(restClient);

        service = new CountryValidationService(builder, "http://country-config");
    }

    @Test
    void validateCountry_liveCountry_returnsDetails() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "CM");
        response.put("launchStatus", "LIVE");
        response.put("registrationEnabled", true);
        response.put("defaultLanguageCode", "fr");
        response.put("defaultTimezone", "Africa/Douala");
        response.put("defaultCurrencyCode", "XAF");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        CountryValidationResult result = service.validateCountry("CM");

        assertNotNull(result);
        assertEquals("CM", result.countryCode());
        assertEquals("LIVE", result.launchStatus());
        assertTrue(result.registrationEnabled());
        assertEquals("fr", result.defaultLanguageCode());
        assertEquals("Africa/Douala", result.defaultTimezone());
        assertEquals("XAF", result.defaultCurrencyCode());
    }

    @Test
    void validateCountry_nullCountryCode_throwsException() {
        ApiException exception = assertThrows(ApiException.class, 
                () -> service.validateCountry(null));
        assertEquals("COUNTRY_REQUIRED", exception.getCode());
    }

    @Test
    void validateRegistrationEligibility_disabledCountry_throwsException() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "XX");
        response.put("launchStatus", "DISABLED");
        response.put("registrationEnabled", false);
        response.put("defaultLanguageCode", "en");
        response.put("defaultTimezone", "UTC");
        response.put("defaultCurrencyCode", "USD");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.validateRegistrationEligibility("XX", false));
        assertEquals("COUNTRY_DISABLED", exception.getCode());
    }

    @Test
    void validateRegistrationEligibility_registrationDisabled_throwsException() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "SN");
        response.put("launchStatus", "LIVE");
        response.put("registrationEnabled", false);
        response.put("defaultLanguageCode", "fr");
        response.put("defaultTimezone", "Africa/Dakar");
        response.put("defaultCurrencyCode", "XOF");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.validateRegistrationEligibility("SN", false));
        assertEquals("REGISTRATION_DISABLED", exception.getCode());
    }

    @Test
    void validateRegistrationEligibility_comingSoon_withoutAllowFlag_throwsException() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "CI");
        response.put("launchStatus", "COMING_SOON");
        response.put("registrationEnabled", true);
        response.put("defaultLanguageCode", "fr");
        response.put("defaultTimezone", "Africa/Abidjan");
        response.put("defaultCurrencyCode", "XOF");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.validateRegistrationEligibility("CI", false));
        assertEquals("COUNTRY_COMING_SOON", exception.getCode());
    }

    @Test
    void validateRegistrationEligibility_comingSoon_withAllowFlag_succeeds() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "CI");
        response.put("launchStatus", "COMING_SOON");
        response.put("registrationEnabled", true);
        response.put("defaultLanguageCode", "fr");
        response.put("defaultTimezone", "Africa/Abidjan");
        response.put("defaultCurrencyCode", "XOF");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        CountryValidationResult result = service.validateRegistrationEligibility("CI", true);

        assertNotNull(result);
        assertEquals("CI", result.countryCode());
        assertEquals("COMING_SOON", result.launchStatus());
    }

    @Test
    void validateCity_validCityInCountry_succeeds() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", UUID.randomUUID().toString());
        response.put("name", "Douala");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        UUID cityId = UUID.randomUUID();
        assertDoesNotThrow(() -> service.validateCity("CM", cityId));
    }

    @Test
    void validateCity_cityOutsideCountry_isRejected() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.validateCity("CM", UUID.randomUUID()));
        assertEquals("CITY_NOT_FOUND_OR_INVALID", exception.getCode());
    }

    @Test
    void validateCity_nullCityId_succeeds() {
        assertDoesNotThrow(() -> service.validateCity("CM", null));
        verify(restClient, never()).get();
    }
}
