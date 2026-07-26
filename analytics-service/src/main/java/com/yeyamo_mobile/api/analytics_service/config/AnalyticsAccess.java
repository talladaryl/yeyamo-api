package com.yeyamo_mobile.api.analytics_service.config;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.analytics_service.repository.PartnerDimensionRepository;

@Component("analyticsAccess")
public class AnalyticsAccess {
    private final PartnerDimensionRepository partners;
    private final RestClient partnerService;

    public AnalyticsAccess(PartnerDimensionRepository partners) {
        this.partners = partners;
        this.partnerService = null;
    }

    @Autowired
    public AnalyticsAccess(PartnerDimensionRepository partners,
            @Value("${yeyamo.services.partner-url:http://partner-service:8080}")
            String partnerUrl) {
        this.partners = partners;
        this.partnerService = RestClient.create(partnerUrl);
    }

    public boolean canReadPartner(Authentication authentication, UUID partnerId) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (partnerService != null) {
            if (!(authentication instanceof JwtAuthenticationToken jwt)) return false;
            try {
                PermissionResponse response = partnerService.get()
                    .uri("/api/v1/partners/{partnerId}/staff/permissions/{permission}",
                        partnerId, "partner:analytics-view")
                    .header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwt.getToken().getTokenValue())
                    .retrieve().body(PermissionResponse.class);
                return response != null && response.allowed();
            } catch (RuntimeException unavailable) {
                return false;
            }
        }
        return partners.findById(partnerId)
                .map(partner -> authentication.getName().equals(partner.getOwnerUserId()))
                .orElse(false);
    }

    private record PermissionResponse(boolean allowed) {}

    public boolean canReadUser(Authentication authentication, String userId) {
        return isAdmin(authentication) || authentication.getName().equals(userId);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private String first(JwtAuthenticationToken jwt, String... claims) {
        for (String claim : claims) {
            Object value = jwt.getToken().getClaim(claim);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }
}
