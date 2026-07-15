package com.yeyamo_mobile.api.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(@NotBlank @Size(max = 8192) String idToken) {
}
