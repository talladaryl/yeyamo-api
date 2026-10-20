package com.yeyamo_mobile.api.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.yeyamo_mobile.api.auth_service.enums.AccountType;

public record OAuthLoginRequest(@NotBlank @Size(max = 8192) String idToken, AccountType accountType) {
    public OAuthLoginRequest(String idToken) {
        this(idToken, AccountType.EXPLORER);
    }
}
