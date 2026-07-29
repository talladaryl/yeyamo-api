package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.yeyamo_mobile.api.auth_service.dto.AdminRevokeSessionsRequest;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserRolesRequest;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserStatusRequest;
import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.event.AuthEventOutbox;
import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.RefreshToken;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.AdminUserProfileProjectionRepository;
import com.yeyamo_mobile.api.auth_service.repository.RefreshTokenRepository;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;

class AdminPlatformUserServiceTests {
    private UserRepository users;
    private RoleRepository roles;
    private RefreshTokenRepository sessions;
    private AdminUserProfileProjectionRepository profiles;
    private AuthEventOutbox outbox;
    private AdminPlatformUserService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); roles = mock(RoleRepository.class);
        sessions = mock(RefreshTokenRepository.class); profiles = mock(AdminUserProfileProjectionRepository.class);
        outbox = mock(AuthEventOutbox.class);
        service = new AdminPlatformUserService(users, roles, sessions, profiles, outbox);
        user = new User(); user.setId(42L); user.setEmail("user@yeyamo.test");
        user.setStatus(UserStatus.ACTIVE); user.setCreatedAt(LocalDateTime.now()); user.setRoles(Set.of());
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(profiles.findByAuthUserIdIn(any())).thenReturn(java.util.List.of());
        when(profiles.findById(42L)).thenReturn(Optional.empty());
    }

    @Test void paginatesAndForwardsFiltersToDatabase() {
        when(users.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(user)));
        var result = service.list("user", null, null, UserStatus.ACTIVE, null, null, null, null, null, null,
                PageRequest.of(0, 20));
        assertEquals(1, result.getTotalElements());
    }

    @Test void suspendsAndRevokesSessionsWithAuditEvent() {
        service.changeStatus(42L, new AdminUserStatusRequest(UserStatus.SUSPENDED, "abus"), actor(), "corr-1");
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        verify(sessions).deleteByUser(user);
        verify(outbox).adminChanged(any(), any(), any(), any(), any(), any());
    }

    @Test void rejectsAdministrativeRoleThroughPlatformEndpoint() {
        assertThrows(ApiException.class, () -> service.changeRoles(42L,
                new AdminUserRolesRequest(Set.of(Roles.SUPER_ADMIN), "escalation"), actor(), "corr-2"));
    }

    @Test void revokesOneSessionWithoutExposingToken() {
        RefreshToken token = new RefreshToken(); token.setId(9L); token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(sessions.findByIdAndUser(9L, user)).thenReturn(Optional.of(token));
        service.revokeSessions(42L, new AdminRevokeSessionsRequest(9L, "security"), actor(), "corr-3");
        verify(sessions).save(token);
    }

    @Test void streamsCsvByDatabasePages() throws Exception {
        when(users.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(user), PageRequest.of(0, 500), 1));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.export(output, null, null, null, null, "100", "corr-export");
        String csv = output.toString(java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("user@yeyamo.test"));
        org.junit.jupiter.api.Assertions.assertFalse(csv.contains("token_hash"));
        verify(outbox).adminExport("100", "corr-export", java.util.Map.of());
    }

    private UsernamePasswordAuthenticationToken actor() {
        return new UsernamePasswordAuthenticationToken("100", null, java.util.List.of());
    }
}
