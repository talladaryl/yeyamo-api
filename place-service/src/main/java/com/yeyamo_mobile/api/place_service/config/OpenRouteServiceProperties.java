package com.yeyamo_mobile.api.place_service.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yeyamo.routing.open-route-service")
public record OpenRouteServiceProperties(
        URI baseUrl,
        String apiKey,
        Duration timeout,
        Duration cacheTtl
) {
    public OpenRouteServiceProperties {
        baseUrl = baseUrl == null ? URI.create("https://api.openrouteservice.org") : baseUrl;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        cacheTtl = cacheTtl == null ? Duration.ofMinutes(5) : cacheTtl;
    }
}
