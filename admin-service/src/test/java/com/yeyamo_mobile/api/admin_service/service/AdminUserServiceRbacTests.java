package com.yeyamo_mobile.api.admin_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.admin_service.dto.CreateAdminUserRequest;
import com.yeyamo_mobile.api.admin_service.dto.UpdateAdminUserRequest;
import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;
import com.yeyamo_mobile.api.admin_service.exception.ApiException;
import com.yeyamo_mobile.api.admin_service.models.AdminUser;
import com.yeyamo_mobile.api.admin_service.repository.AdminUserRepository;

import jakarta.servlet.http.HttpServletRequest;

class AdminUserServiceRbacTests {
    private AdminUserRepository repository; private AuditService audit;
    private AdminUserService service; private HttpServletRequest request;

    @BeforeEach void setUp() {
        repository = mock(AdminUserRepository.class); audit = mock(AuditService.class);
        request = mock(HttpServletRequest.class); service = new AdminUserService(repository, audit);
        when(audit.currentAdminId()).thenReturn(UUID.randomUUID());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void rejectsDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("admin@yeyamo.test")).thenReturn(true);
        assertThrows(ApiException.class, () -> service.create(new CreateAdminUserRequest(UUID.randomUUID(), "Admin",
                "admin@yeyamo.test", AdminRole.ADMIN, Set.of(), Set.of(), AdminStatus.ACTIVE), request));
    }

    @Test void rejectsUnknownPermissionEscalation() {
        assertThrows(ApiException.class, () -> service.create(new CreateAdminUserRequest(UUID.randomUUID(), "Admin",
                "admin@yeyamo.test", AdminRole.ADMIN, Set.of("root:everything"), Set.of(), AdminStatus.ACTIVE), request));
    }

    @Test void protectsLastActiveSuperAdmin() {
        AdminUser admin = admin(AdminRole.SUPER_ADMIN, AdminStatus.ACTIVE);
        when(repository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(repository.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminStatus.ACTIVE)).thenReturn(1L);
        assertThrows(ApiException.class, () -> service.update(admin.getId(), new UpdateAdminUserRequest("Root",
                "root@yeyamo.test", AdminRole.ADMIN, Set.of(), Set.of(), AdminStatus.ACTIVE), request));
    }

    @Test void recordsCreateAudit() {
        service.create(new CreateAdminUserRequest(UUID.randomUUID(), "Admin", "admin@yeyamo.test", AdminRole.ADMIN,
                Set.of("users:read"), Set.of(), AdminStatus.ACTIVE), request);
        verify(audit).record(org.mockito.ArgumentMatchers.eq("ADMIN_CREATED"), any(), any(), any(), any());
    }

    private AdminUser admin(AdminRole role, AdminStatus status) {
        AdminUser admin = new AdminUser(); admin.setName("Root"); admin.setEmail("root@yeyamo.test");
        admin.setUserId(UUID.randomUUID()); admin.setRole(role); admin.setStatus(status); return admin;
    }
}
