package com.yeyamo_mobile.api.ads_delivery_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdsDeliveryServiceApplicationTests {

    @Autowired MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void actuatorHealthProbesArePublicButAdvertisingRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ads/select")).andExpect(status().isUnauthorized());
    }
}
