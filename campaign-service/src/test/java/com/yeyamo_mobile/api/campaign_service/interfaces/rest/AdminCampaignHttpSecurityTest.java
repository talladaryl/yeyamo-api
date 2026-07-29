package com.yeyamo_mobile.api.campaign_service.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yeyamo_mobile.api.campaign_service.application.CampaignService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCampaignHttpSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/campaigns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedAdministratorWithoutCampaignScope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/campaigns")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsCampaignApprovalScope() throws Exception {
        when(campaignService.listAllCampaigns(isNull(), any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/api/v1/admin/campaigns")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_campaign:approve"))))
                .andExpect(status().isOk());
    }
}
