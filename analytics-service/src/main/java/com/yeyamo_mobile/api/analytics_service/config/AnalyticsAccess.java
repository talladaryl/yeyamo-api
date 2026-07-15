package com.yeyamo_mobile.api.analytics_service.config;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.repository.PartnerDimensionRepository;

@Component("analyticsAccess")
public class AnalyticsAccess {
    private final PartnerDimensionRepository partners;

    public AnalyticsAccess(PartnerDimensionRepository partners) {
        this.partners = partners;
    }

    public boolean canReadPartner(Authentication authentication, UUID partnerId) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String claim = first(jwt, "partnerId", "partner_id");
            if (partnerId.toString().equals(claim)) {
                return true;
            }
        }
        return partners.findById(partnerId)
                .map(partner -> authentication.getName().equals(partner.getOwnerUserId()))
                .orElse(false);
    }

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
