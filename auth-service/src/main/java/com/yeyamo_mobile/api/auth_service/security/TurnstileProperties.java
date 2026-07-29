package com.yeyamo_mobile.api.auth_service.security;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.turnstile")
public record TurnstileProperties(
        boolean enabled,
        String secretKey,
        URI verifyUrl,
        String expectedHostname,
        Duration timeout
) {
    public TurnstileProperties {
        verifyUrl = verifyUrl == null
                ? URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify")
                : verifyUrl;
        timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
    }
}
