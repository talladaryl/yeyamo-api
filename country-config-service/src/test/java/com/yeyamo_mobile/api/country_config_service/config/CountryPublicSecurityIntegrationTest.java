package com.yeyamo_mobile.api.country_config_service.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/v1/countries/available"))
                .andExpect(status().isOk());
    }
}
