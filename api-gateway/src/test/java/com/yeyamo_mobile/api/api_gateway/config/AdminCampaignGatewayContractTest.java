package com.yeyamo_mobile.api.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AdminCampaignGatewayContractTest {
    @Test
    void campaignAdminRouteIsMoreSpecificAndPrioritized() throws IOException {
        String properties = Files.readString(gatewayProperties());
        assertThat(properties).contains("routes[25].id=campaign-service");
        assertThat(properties).contains("routes[25].order=-100");
        assertThat(properties).contains("Path=/api/v1/campaigns/**,/api/v1/admin/campaigns/**");
        assertThat(properties.indexOf("routes[4].id=admin-service"))
                .isLessThan(properties.indexOf("routes[25].id=campaign-service"));
    }

    @Test
    void convertsRolesScopesAndPermissionsWithoutDoublePrefixes() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of(
                        "sub", "42",
                        "roles", List.of("ADMIN"),
                        "scope", "campaign:approve campaign:read",
                        "scopes", List.of("campaign:reject"),
                        "permissions", List.of("users:suspend")));
        var authentication = new SecurityConfig().authoritiesConverter().convert(jwt);
        assertThat(authentication.getAuthorities()).extracting("authority").contains(
                "ROLE_ADMIN", "SCOPE_campaign:approve", "SCOPE_campaign:read",
                "SCOPE_campaign:reject", "PERMISSION_users:suspend");
    }

    @Test
    void platformUsersRouteTargetsAuthBeforeGenericAdmin() throws IOException {
        String properties = Files.readString(gatewayProperties());
        assertThat(properties).contains("routes[28].id=auth-service-admin-platform-users");
        assertThat(properties).contains("routes[28].order=-110");
        assertThat(properties).contains("Path=/api/v1/admin/platform-users/**,/api/v1/admin/platform-users");
        assertThat(properties).contains("routes[28].uri=lb://auth-service");
    }

    @Test
    void xpLedgerRouteTargetsGamificationBeforeGenericAdmin() throws IOException {
        String properties = Files.readString(gatewayProperties());
        assertThat(properties).contains("routes[15].id=gamification-service");
        assertThat(properties).contains("routes[15].order=-100");
        assertThat(properties).contains("/api/v1/admin/gamification/**");
        assertThat(properties).contains("routes[15].uri=lb://gamification-service");
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
