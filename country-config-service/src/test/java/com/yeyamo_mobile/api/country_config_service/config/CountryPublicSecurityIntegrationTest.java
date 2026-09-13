package com.yeyamo_mobile.api.country_config_service.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CountryPublicSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void countryDiscoveryIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk());

        // The registration flow must remain public even when a client carries
        // an expired bearer value from a previous session.
        mockMvc.perform(get("/api/v1/countries")
                .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/countries/available"))
                .andExpect(status().isOk());
    }

    @Test
    void countryMutationsAndAdminRoutesRemainProtected() throws Exception {
        // There is no public country mutation controller. The country-specific
        // chain must deny an attempted mutation rather than exposing it.
        mockMvc.perform(post("/api/v1/countries"))
                .andExpect(status().isForbidden());

        // Actual mutations are admin routes and must still require a JWT.
        mockMvc.perform(put("/api/v1/admin/countries/CM"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/countries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthRemainsPublicWithoutOpeningOtherActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
