package com.yeyamo_mobile.api.place_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionResponse;
import com.yeyamo_mobile.api.place_service.service.PlaceSuggestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/place-suggestions")
public class PlaceSuggestionController {
    private final PlaceSuggestionService service;
    public PlaceSuggestionController(PlaceSuggestionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceSuggestionResponse create(@Valid @RequestBody PlaceSuggestionRequest request, Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @GetMapping("/me")
    public Page<PlaceSuggestionResponse> mine(Authentication authentication, org.springframework.data.domain.Pageable pageable) {
        return service.mine(authentication.getName(), pageable);
    }
}
