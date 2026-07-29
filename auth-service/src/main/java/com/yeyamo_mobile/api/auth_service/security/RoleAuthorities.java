package com.yeyamo_mobile.api.auth_service.security;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.yeyamo_mobile.api.auth_service.enums.Roles;

final class RoleAuthorities {
    private static final Set<String> CAMPAIGN_ADMIN = Set.of(
            "campaign:read", "campaign:approve", "campaign:reject");
    private static final Set<String> CAMPAIGN_OWNER = Set.of(
            "campaign:read", "campaign:create", "campaign:update", "campaign:submit", "campaign:pause");
    private static final Map<Roles, Set<String>> SCOPES = scopesByRole();

    private RoleAuthorities() {
    }

    static Set<String> scopes(Set<Roles> roles) {
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        roles.forEach(role -> scopes.addAll(SCOPES.getOrDefault(role, Set.of())));
        return Set.copyOf(scopes);
    }

    static Set<String> permissions(Set<Roles> roles) {
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        if (roles.contains(Roles.SUPER_ADMIN)) {
            permissions.add("admin:manage");
            permissions.add("finance:adjust");
            permissions.add("users:suspend");
        }
        if (roles.contains(Roles.ADMIN)) {
            permissions.add("admin:read");
            permissions.add("users:suspend");
        }
        if (roles.contains(Roles.MODERATOR)) {
            permissions.add("moderation:decide");
        }
        if (roles.contains(Roles.SUPPORT)) {
            permissions.add("support:manage");
        }
        return Set.copyOf(permissions);
    }

    private static Map<Roles, Set<String>> scopesByRole() {
        EnumMap<Roles, Set<String>> scopes = new EnumMap<>(Roles.class);
        scopes.put(Roles.SUPER_ADMIN, union(CAMPAIGN_ADMIN, CAMPAIGN_OWNER));
        scopes.put(Roles.ADMIN, union(CAMPAIGN_ADMIN, CAMPAIGN_OWNER));
        scopes.put(Roles.COMMERCIAL, Set.of("campaign:read"));
        scopes.put(Roles.PARTNER, CAMPAIGN_OWNER);
        return Map.copyOf(scopes);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        LinkedHashSet<String> values = new LinkedHashSet<>(first);
        values.addAll(second);
        return Set.copyOf(values);
    }
}
