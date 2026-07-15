package com.yeyamo_mobile.api.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "\\+?[0-9]{8,15}") String phone,
        @Size(min = 12, max = 128) String password,
        @Size(max = 80) @Pattern(regexp = "[\\p{L}\\p{N} .'-]*") String displayName
) {
}
