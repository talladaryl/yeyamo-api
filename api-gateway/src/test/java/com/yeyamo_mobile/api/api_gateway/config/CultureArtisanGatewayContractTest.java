package com.yeyamo_mobile.api.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CultureArtisanGatewayContractTest {

    @Test
    void cultureAndArtisanRoutesUseTheirOwningServices() throws IOException {
        String properties = Files.readString(gatewayProperties());

        assertThat(properties).contains("routes[30].uri=lb://culture-service")
                .contains("Path=/api/v1/culture/**,/api/v1/admin/culture/**")
                .contains("routes[31].uri=lb://graph-service")
                .contains("Path=/api/v1/culture-graph/**,/api/v1/admin/culture-graph/**")
                .contains("routes[32].uri=lb://catalog-service")
                .contains("Path=/api/v1/artworks/**")
                .contains("routes[33].uri=lb://catalog-service")
                .contains("Path=/api/v1/artisans/*/artworks")
                .contains("routes[34].uri=lb://partner-service")
                .contains("Path=/api/v1/artisans/**,/api/v1/admin/artisans/**,/api/v1/artisan-specialties/**,/api/v1/partners/me/artisan-profile")
                .contains("routes[35].uri=lb://commerce-service")
                .contains("Path=/api/v1/artwork-orders/**,/api/v1/artisan/orders/**")
                .contains("routes[36].uri=lb://commerce-service")
                .contains("Path=/api/v1/artwork-offers/**");
    }

    @Test
    void specialisedRateLimitValuesAreConfigured() throws IOException {
        String properties = Files.readString(gatewayProperties());
        assertThat(properties).contains("security.rate-limit.culture.upload-audio.requests")
                .contains("security.rate-limit.culture.upload-artwork.requests")
                .contains("security.rate-limit.culture.contributions.requests")
                .contains("security.rate-limit.culture.translations.requests")
                .contains("security.rate-limit.culture.quiz-attempts.requests")
                .contains("security.rate-limit.culture.orders.requests")
                .contains("security.rate-limit.culture.reporting.requests");
    }

    private Path gatewayProperties() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 3 && current != null; depth++, current = current.getParent()) {
            Path candidate = current.resolve("cloud-conf-yeyamo/api-gateway.properties");
            if (Files.exists(candidate)) return candidate;
        }
        throw new IllegalStateException("cloud-conf-yeyamo/api-gateway.properties not found");
    }
}
