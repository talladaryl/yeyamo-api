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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.yeyamo_mobile.api.event_service.dto.EventRequest;
import com.yeyamo_mobile.api.event_service.dto.EventResponse;
import com.yeyamo_mobile.api.event_service.dto.EventStatusRequest;
import com.yeyamo_mobile.api.event_service.dto.EventSummaryResponse;
import com.yeyamo_mobile.api.event_service.dto.EventUpdateRequest;
import com.yeyamo_mobile.api.event_service.exception.ApiException;
import com.yeyamo_mobile.api.event_service.service.EventService;

import jakarta.validation.Valid;

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

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable UUID id) {
        return eventService.getById(id);
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
        return eventService.updateStatus(id, request, correlationId, authentication.getName());
    }

    @PostMapping("/{id}/register")
    @ResponseStatus(HttpStatus.OK)
    public EventResponse register(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return eventService.register(id, parseUserId(authentication.getName()));
    }

    @DeleteMapping("/{id}/unregister")
    public EventResponse unregister(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return eventService.unregister(id, parseUserId(authentication.getName()));
    }

    private UUID parseUserId(String userIdHeader) {
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException exception) {
            throw new ApiException("INVALID_USER_ID", "Identifiant utilisateur invalide", HttpStatus.BAD_REQUEST);
        }
    }
}
