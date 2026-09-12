package com.yeyamo_mobile.api.place_service.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionModerationRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRejectionRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionResponse;
import com.yeyamo_mobile.api.place_service.models.PlaceSuggestion;
import com.yeyamo_mobile.api.place_service.service.PlaceSuggestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/place-suggestions")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
public class AdminPlaceSuggestionController {
    private final PlaceSuggestionService service;
    public AdminPlaceSuggestionController(PlaceSuggestionService service) { this.service = service; }

    @GetMapping
    public Page<PlaceSuggestionResponse> list(@RequestParam(required = false) PlaceSuggestion.Status status,
            org.springframework.data.domain.Pageable pageable) {
        return service.moderateList(status, pageable);
    }

    @PatchMapping("/{id}/approve")
    public PlaceSuggestionResponse approve(@PathVariable UUID id, @Valid @RequestBody PlaceSuggestionModerationRequest request,
            Authentication authentication) {
        return service.approve(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/reject")
    public PlaceSuggestionResponse reject(@PathVariable UUID id, @Valid @RequestBody PlaceSuggestionRejectionRequest request,
            Authentication authentication) {
        return service.reject(id, request, authentication.getName());
    }
}
