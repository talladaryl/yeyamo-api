package com.yeyamo_mobile.api.auth_service.security;

public interface AntiBotVerifier {
    void verify(String token, AntiBotAction action, String clientIp);
}
