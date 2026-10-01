package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Batches viewer-aware post counters to prevent one HTTP request per Feed row. */
public record PostSummariesRequest(
        @NotEmpty @Size(max = 50) List<@NotNull UUID> postIds) {
}
