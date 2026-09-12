package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.util.UUID;

import com.yeyamo_mobile.api.interaction_service.domain.model.ReviewTargetType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifiedReviewRequest(
        @NotNull ReviewTargetType targetType,
        @NotNull UUID targetId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 5000) String comment
) { }
