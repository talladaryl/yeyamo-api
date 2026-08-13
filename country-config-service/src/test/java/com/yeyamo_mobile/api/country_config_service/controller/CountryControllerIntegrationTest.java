package com.yeyamo_mobile.api.country_config_service.controller;

import com.yeyamo_mobile.api.country_config_service.domain.model.Country;
import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;
import com.yeyamo_mobile.api.country_config_service.domain.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CountryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {
        countryRepository.deleteAll();

        // Cameroon LIVE
        Country cameroon = new Country(
                "CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237",
                CountryLaunchStatus.LIVE
        );
        cameroon.setRegistrationEnabled(true);
        cameroon.setPaymentsEnabled(true);
        countryRepository.save(cameroon);

        // Nigeria COMING_SOON
        Country nigeria = new Country(
                "NG", "Nigeria", "Federal Republic of Nigeria", "AF",
                "en", "NGN", "Africa/Lagos", "+234",
                CountryLaunchStatus.COMING_SOON
        );
        countryRepository.save(nigeria);
    }

    @Test
    @DisplayName("GET /api/v1/countries - Should return all countries")
    void shouldReturnAllCountries() throws Exception {
        mockMvc.perform(get("/api/v1/countries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("CM", "NG")));
    }

    @Test
    @DisplayName("GET /api/v1/countries/available - Should return only LIVE countries")
    void shouldReturnOnlyAvailableCountries() throws Exception {
        mockMvc.perform(get("/api/v1/countries/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("CM")))
                .andExpect(jsonPath("$[0].launchStatus", is("LIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/countries/CM - Should return Cameroon")
    void shouldReturnCameroon() throws Exception {
        mockMvc.perform(get("/api/v1/countries/CM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("CM")))
                .andExpect(jsonPath("$.name", is("Cameroon")))
                .andExpect(jsonPath("$.launchStatus", is("LIVE")))
                .andExpect(jsonPath("$.defaultCurrencyCode", is("XAF")))
                .andExpect(jsonPath("$.phoneCountryCode", is("+237")))
                .andExpect(jsonPath("$.registrationEnabled", is(true)))
                .andExpect(jsonPath("$.paymentsEnabled", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/countries/XX - Should return 404 for unknown country")
    void shouldReturn404ForUnknownCountry() throws Exception {
        mockMvc.perform(get("/api/v1/countries/XX")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Country Not Found")));
    }

    @Test
    @DisplayName("GET /api/v1/countries/CM/features - Should return features")
    void shouldReturnCountryFeatures() throws Exception {
        mockMvc.perform(get("/api/v1/countries/CM/features")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationEnabled", is(true)))
                .andExpect(jsonPath("$.paymentsEnabled", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/countries/NG - Should return Nigeria as COMING_SOON")
    void shouldReturnNigeriaAsComingSoon() throws Exception {
        mockMvc.perform(get("/api/v1/countries/NG")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("NG")))
                .andExpect(jsonPath("$.launchStatus", is("COMING_SOON")))
                .andExpect(jsonPath("$.registrationEnabled", is(false)));
    }
}
