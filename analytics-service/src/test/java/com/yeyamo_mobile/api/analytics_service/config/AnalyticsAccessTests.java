package com.yeyamo_mobile.api.analytics_service.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.yeyamo_mobile.api.analytics_service.models.PartnerDimension;
import com.yeyamo_mobile.api.analytics_service.repository.PartnerDimensionRepository;

class AnalyticsAccessTests {

    @Test
    void partnerOwnerCanReadOnlyTheOwnedPartnerProjection() {
        UUID ownedId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        PartnerDimension owned = new PartnerDimension();
        owned.setPartnerId(ownedId);
        owned.setOwnerUserId("owner-1");
        PartnerDimension other = new PartnerDimension();
        other.setPartnerId(otherId);
        other.setOwnerUserId("owner-2");
        PartnerDimensionRepository partners = mock(PartnerDimensionRepository.class);
        when(partners.findById(ownedId)).thenReturn(Optional.of(owned));
        when(partners.findById(otherId)).thenReturn(Optional.of(other));
        AnalyticsAccess access = new AnalyticsAccess(partners);
        var authentication = new UsernamePasswordAuthenticationToken("owner-1", "token",
                List.of(new SimpleGrantedAuthority("ROLE_PARTNER")));

        assertTrue(access.canReadPartner(authentication, ownedId));
        assertFalse(access.canReadPartner(authentication, otherId));
    }

    @Test
    void administratorCanReadAnyUserProjection() {
        AnalyticsAccess access = new AnalyticsAccess(mock(PartnerDimensionRepository.class));
        var authentication = new UsernamePasswordAuthenticationToken("admin", "token",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertTrue(access.canReadUser(authentication, "another-user"));
    }
}
