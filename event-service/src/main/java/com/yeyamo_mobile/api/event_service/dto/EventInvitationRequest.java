package com.yeyamo_mobile.api.event_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventInvitationRequest(@NotBlank @Size(max = 120) String userId) { }
