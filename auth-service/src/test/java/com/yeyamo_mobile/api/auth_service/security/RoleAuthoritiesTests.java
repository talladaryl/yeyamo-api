package com.yeyamo_mobile.api.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.auth_service.enums.Roles;

class RoleAuthoritiesTests {
    @Test
    void grantsCampaignApprovalScopesToAdministrators() {
        assertThat(RoleAuthorities.scopes(Set.of(Roles.ADMIN)))
                .contains("campaign:read", "campaign:approve", "campaign:reject");
    }

    @Test
    void doesNotGrantApprovalScopesToCommercialUsers() {
        assertThat(RoleAuthorities.scopes(Set.of(Roles.COMMERCIAL)))
                .containsExactly("campaign:read");
    }

    @Test
    void keepsSupportPermissionsSeparateFromScopes() {
        assertThat(RoleAuthorities.permissions(Set.of(Roles.SUPPORT))).contains("support:manage");
        assertThat(RoleAuthorities.scopes(Set.of(Roles.SUPPORT))).isEmpty();
    }
}
