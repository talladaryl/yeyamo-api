package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import jakarta.validation.constraints.*;

public record ReviewRequest(
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    Integer rating,
    
    @Size(max = 5000, message = "Comment must not exceed 5000 characters")
    String comment
) {}
