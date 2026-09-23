package com.yeyamo_mobile.api.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FeedGatewayContractTest {

    @Test
    void routesBothPersonalizedAndPublicFeedToFeedService() throws IOException {
        String properties = Files.readString(gatewayProperties());

        assertThat(properties)
                .contains("routes[12].id=feed-service")
                .contains("routes[12].uri=lb://feed-service")
                .contains("Path=/api/v1/feed/**,/api/v1/feed,/api/v1/public/feed/**,/api/v1/public/feed");
    }

    private Path gatewayProperties() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 3 && current != null; depth++, current = current.getParent()) {
            Path candidate = current.resolve("cloud-conf-yeyamo/api-gateway.properties");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("cloud-conf-yeyamo/api-gateway.properties not found");
    }
}
