package com.yeyamo_mobile.api.event_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.yeyamo_mobile.api.event_service.dto.EventRequest;
import com.yeyamo_mobile.api.event_service.dto.EventParticipantResponse;
import com.yeyamo_mobile.api.event_service.dto.EventResponse;
import com.yeyamo_mobile.api.event_service.dto.EventStatusRequest;
import com.yeyamo_mobile.api.event_service.dto.EventSummaryResponse;
import com.yeyamo_mobile.api.event_service.dto.EventUpdateRequest;
import com.yeyamo_mobile.api.event_service.dto.EventInvitationRequest;
import com.yeyamo_mobile.api.event_service.dto.EventInvitationResponse;
import com.yeyamo_mobile.api.event_service.service.EventService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/events")
@Validated
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(
            @Valid @RequestBody EventRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication authentication
    ) {
        return eventService.create(request, correlationId, authentication.getName());
    }

    @GetMapping("/upcoming")
    public List<EventSummaryResponse> upcoming() {
        return eventService.findUpcoming();
    }

    /**
     * Real composition endpoint for a Place detail. It returns only published
     * events, preventing mobile clients from fabricating related-event cards.
     */
    @GetMapping
    public List<EventSummaryResponse> byPlace(
            @RequestParam UUID placeId
    ) {
        return eventService.findByPlaceId(placeId, true);
    }

    @GetMapping("/me")
    public List<EventSummaryResponse> mine(
            Authentication authentication,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return eventService.findRegisteredByUser(authentication.getName(), limit);
    }

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable UUID id, Authentication authentication) {
        return eventService.getById(id, authentication == null ? null : authentication.getName(), admin(authentication));
    }

    @GetMapping("/{id}/participants")
    public List<EventParticipantResponse> participants(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit) {
        return eventService.findParticipants(id, limit, authentication == null ? null : authentication.getName(), admin(authentication));
    }

    @PutMapping("/{id}")
    public EventResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody EventUpdateRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication authentication
    ) {
        return eventService.update(id, request, correlationId, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    public EventResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody EventStatusRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication authentication
    ) {
        return eventService.updateStatus(id, request, correlationId, authentication.getName(), admin(authentication));
    }

    @PostMapping("/{id}/register")
    @ResponseStatus(HttpStatus.OK)
    public EventResponse register(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return eventService.register(id, authentication.getName());
    }

    @DeleteMapping("/{id}/unregister")
    public EventResponse unregister(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return eventService.unregister(id, authentication.getName());
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public EventInvitationResponse invite(@PathVariable UUID id, @Valid @RequestBody EventInvitationRequest request,
            Authentication authentication) {
        return eventService.invite(id, request.userId(), authentication.getName(), admin(authentication));
    }

    @GetMapping("/{id}/invitations")
    public List<EventInvitationResponse> invitations(@PathVariable UUID id, Authentication authentication) {
        return eventService.invitations(id, authentication.getName(), admin(authentication));
    }

    @DeleteMapping("/{id}/invitations/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@PathVariable UUID id, @PathVariable String userId, Authentication authentication) {
        eventService.revokeInvitation(id, userId, authentication.getName(), admin(authentication));
    }

    private boolean admin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(value -> "ROLE_ADMIN".equals(value.getAuthority()) || "ROLE_SUPER_ADMIN".equals(value.getAuthority()));
    }
}
