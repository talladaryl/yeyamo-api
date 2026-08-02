package com.yeyamo_mobile.api.campaign_service.interfaces.rest;

import com.yeyamo_mobile.api.campaign_service.application.CampaignService;
import com.yeyamo_mobile.api.campaign_service.application.dto.*;
import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {
    
    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_campaign:create')")
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.createCampaign(request, partnerId, actorId, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_campaign:update')")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignRequest request,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.updateDraftCampaign(id, request, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('SCOPE_campaign:submit')")
    public ResponseEntity<CampaignResponse> submitCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.submitCampaign(id, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SCOPE_campaign:update')")
    public ResponseEntity<CampaignResponse> activateCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.activateCampaign(id, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('SCOPE_campaign:pause')")
    public ResponseEntity<CampaignResponse> pauseCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.pauseCampaign(id, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('SCOPE_campaign:update')")
    public ResponseEntity<CampaignResponse> resumeCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.resumeCampaign(id, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_campaign:update')")
    public ResponseEntity<CampaignResponse> cancelCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.cancelCampaign(id, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SCOPE_campaign:update')")
    public ResponseEntity<CampaignResponse> completeCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        String actorId = authentication.getName();
        
        CampaignResponse response = service.completeCampaign(id, partnerId, actorId, correlationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_campaign:read')")
    public ResponseEntity<CampaignResponse> getCampaign(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        CampaignResponse response = service.getCampaign(id, partnerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_campaign:read')")
    public ResponseEntity<Page<CampaignResponse>> listCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            JwtAuthenticationToken authentication,
            Pageable pageable) {
        
        String partnerId = authentication.getToken().getClaimAsString("partner_id");
        Page<CampaignResponse> response = service.listPartnerCampaigns(partnerId, status, pageable);
        return ResponseEntity.ok(response);
    }
}
