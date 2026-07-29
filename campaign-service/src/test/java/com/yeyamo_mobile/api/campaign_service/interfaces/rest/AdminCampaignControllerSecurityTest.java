package com.yeyamo_mobile.api.campaign_service.interfaces.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.yeyamo_mobile.api.campaign_service.application.CampaignService;

class AdminCampaignControllerSecurityTest {
    @Test
    void approvalActorAlwaysComesFromAuthentication() {
        CampaignService service = mock(CampaignService.class);
        AdminCampaignController controller = new AdminCampaignController(service);
        UUID campaignId = UUID.randomUUID();

        controller.approveCampaign(campaignId,
                new UsernamePasswordAuthenticationToken("jwt-subject-91", "n/a"), "corr-123");

        verify(service).approveCampaign(campaignId, "jwt-subject-91", "jwt-subject-91", "corr-123");
    }
}
