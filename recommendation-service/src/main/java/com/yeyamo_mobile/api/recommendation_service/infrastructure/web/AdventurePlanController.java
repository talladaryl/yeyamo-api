package com.yeyamo_mobile.api.recommendation_service.infrastructure.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanDetailResponse;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanPreviewResponse;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanRequest;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanService;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanSummaryResponse;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventureSkipResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

/** Authenticated Explorer planning API. Preview is intentionally authenticated too: ranking uses the JWT profile. */
@Validated
@RestController
@RequestMapping("/api/v1/explore/adventure-plans")
@SecurityRequirement(name = "bearerAuth")
public class AdventurePlanController {
    private final AdventurePlanService service;

    public AdventurePlanController(AdventurePlanService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview an Adventure Plan without persisting it")
    public AdventurePlanPreviewResponse preview(Authentication authentication, @Valid @RequestBody AdventurePlanRequest request) {
        return service.preview(authentication.getName(), request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Persist a server-recalculated Adventure Plan")
    public AdventurePlanDetailResponse create(Authentication authentication, @Valid @RequestBody AdventurePlanRequest request) {
        return service.create(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's Adventure Plans")
    public Page<AdventurePlanSummaryResponse> list(Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        int size = Math.max(1, Math.min(50, pageable.getPageSize()));
        Pageable safePage = PageRequest.of(Math.max(0, pageable.getPageNumber()), size, pageable.getSort());
        return service.list(authentication.getName(), safePage);
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get an Adventure Plan owned by the authenticated user")
    public AdventurePlanDetailResponse get(Authentication authentication, @PathVariable UUID planId) {
        return service.get(authentication.getName(), planId);
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an Adventure Plan owned by the authenticated user")
    public void delete(Authentication authentication, @PathVariable UUID planId) {
        service.delete(authentication.getName(), planId);
    }

    @PostMapping("/{planId}/recommendations/{recommendationId}/skip")
    @Operation(summary = "Persist a plan-local skip and attempt a compatible replacement")
    public AdventureSkipResponse skip(Authentication authentication, @PathVariable UUID planId,
            @PathVariable UUID recommendationId) {
        return service.skip(authentication.getName(), planId, recommendationId);
    }
}
