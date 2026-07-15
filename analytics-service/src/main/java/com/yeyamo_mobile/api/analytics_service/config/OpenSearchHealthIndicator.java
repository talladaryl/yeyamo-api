package com.yeyamo_mobile.api.analytics_service.config;

import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OpenSearchHealthIndicator implements HealthIndicator {
    private final RestHighLevelClient client;

    public OpenSearchHealthIndicator(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            boolean available = client.ping(RequestOptions.DEFAULT);
            return available ? Health.up().withDetail("engine", "OpenSearch").build()
                    : Health.down().withDetail("engine", "OpenSearch").build();
        } catch (Exception exception) {
            return Health.down(exception).withDetail("engine", "OpenSearch").build();
        }
    }
}
