package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.util.Set;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateInterestsRequest(
        @NotNull @Size(max = 30) Set<@Size(min = 1, max = 100) String> categoryCodes,
        boolean completeOnboarding) {}
