package com.yeyamo_mobile.api.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AuthCountryGatewayContractTest {

    @Test
    void authAndCountriesUseLoadBalancedCircuitBreakerRoutes() throws IOException {
        String properties = Files.readString(gatewayProperties());

        assertThat(properties)
                .contains("routes[0].id=auth-service")
                .contains("routes[0].uri=lb://auth-service")
                .contains("routes[0].predicates[0]=Path=/api/v1/auth/**")
                .contains("routes[0].filters[0].args.id=authService")
                .contains("routes[39].id=country-config-service")
                .contains("routes[39].uri=lb://country-config-service")
                .contains("routes[39].predicates[0]=Path=/api/v1/countries/**")
                .contains("routes[39].filters[0].args.id=countryConfigService");
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
