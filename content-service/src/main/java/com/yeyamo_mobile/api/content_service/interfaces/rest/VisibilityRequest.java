package com.yeyamo_mobile.api.content_service.interfaces.rest;
import com.yeyamo_mobile.api.content_service.domain.model.PostVisibility;import jakarta.validation.constraints.NotNull;
public record VisibilityRequest(@NotNull PostVisibility visibility){}
