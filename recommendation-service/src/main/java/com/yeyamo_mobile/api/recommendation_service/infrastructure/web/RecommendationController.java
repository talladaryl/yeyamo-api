package com.yeyamo_mobile.api.recommendation_service.infrastructure.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.recommendation_service.application.*;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationQueryService service;

    public RecommendationController(RecommendationQueryService s) { service = s; }

    @GetMapping
    @Operation(summary = "Get personalized recommendations", security = @SecurityRequirement(name = "bearerAuth"))
    public RecommendationPage recommendations(
            Authentication auth,
            @RequestParam(required = false, name = "lat")    Double latitude,
            @RequestParam(required = false, name = "lng")    Double longitude,
            @RequestParam(required = false)                  String languageCodes,
            @RequestParam(required = false, name = "ctx")    String recommendationContext,
            @RequestParam(defaultValue = "0")                int page,
            @RequestParam(defaultValue = "20")               int size,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {

        List<String> langs = languageCodes != null && !languageCodes.isBlank()
                ? Arrays.asList(languageCodes.split(","))
                : List.of();

        return service.recommend(
                auth.getName(),
                new RecommendationContext(latitude, longitude, langs, recommendationContext),
                page, size, correlation);
    }
}
