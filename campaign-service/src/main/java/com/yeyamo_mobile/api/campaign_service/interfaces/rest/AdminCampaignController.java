package com.yeyamo_mobile.api.campaign_service.interfaces.rest;

import com.yeyamo_mobile.api.campaign_service.application.CampaignService;
import com.yeyamo_mobile.api.campaign_service.application.dto.CampaignResponse;
import com.yeyamo_mobile.api.campaign_service.application.dto.RejectCampaignRequest;
import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
public class AdminCampaignController {
    
    private final CampaignService service;

    public AdminCampaignController(CampaignService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_campaign:approve') or hasAuthority('SCOPE_campaign:reject')")
    public ResponseEntity<Page<CampaignResponse>> listAllCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            Pageable pageable) {
        
        Page<CampaignResponse> response = service.listAllCampaigns(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_campaign:approve') or hasAuthority('SCOPE_campaign:reject')")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable UUID id) {
        CampaignResponse response = service.getCampaignAdmin(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_campaign:approve')")
    public ResponseEntity<CampaignResponse> approveCampaign(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String actorId = authentication.getName();
        CampaignResponse response = service.approveCampaign(id, actorId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SCOPE_campaign:reject')")
    public ResponseEntity<CampaignResponse> rejectCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody RejectCampaignRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String actorId = authentication.getName();
        CampaignResponse response = service.rejectCampaign(id, request, actorId, correlationId);
        return ResponseEntity.ok(response);
    }
}
