package com.yeyamo_mobile.api.admin_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.admin_service.dto.AdminUserRequest;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;
import com.yeyamo_mobile.api.admin_service.exception.ApiException;
import com.yeyamo_mobile.api.admin_service.models.AdminUser;
import com.yeyamo_mobile.api.admin_service.repository.AdminUserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AdminUserService {

    private final AdminUserRepository repository;
    private final AuditService auditService;

    public AdminUserService(AdminUserRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUser> list() {
        return repository.findAll();
    }

    @Transactional
    public AdminUser create(AdminUserRequest request, HttpServletRequest httpRequest) {
        if (repository.existsByUserId(request.userId())) {
            throw new ApiException("ADMIN_USER_EXISTS", "Cet utilisateur est deja administrateur", HttpStatus.CONFLICT);
        }
        AdminUser adminUser = new AdminUser();
        adminUser.setUserId(request.userId());
        adminUser.setRole(request.role());
        adminUser.setPermissions(request.safePermissions());
        adminUser.setStatus(request.status() == null ? AdminStatus.ACTIVE : request.status());
        adminUser.setCreatedBy(auditService.currentAdminId());
        AdminUser saved = repository.save(adminUser);
        auditService.record("CREATE_ADMIN_USER", "ADMIN_USER", saved.getId(), request.safePermissions(), httpRequest);
        return saved;
    }

    @Transactional
    public AdminUser update(UUID id, AdminUserRequest request, HttpServletRequest httpRequest) {
        AdminUser adminUser = get(id);
        adminUser.setRole(request.role());
        adminUser.setPermissions(request.safePermissions());
        adminUser.setStatus(request.status() == null ? adminUser.getStatus() : request.status());
        AdminUser saved = repository.save(adminUser);
        auditService.record("UPDATE_ADMIN_USER", "ADMIN_USER", saved.getId(), request.safePermissions(), httpRequest);
        return saved;
    }

    @Transactional(readOnly = true)
    public AdminUser get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException("ADMIN_USER_NOT_FOUND", "Administrateur introuvable", HttpStatus.NOT_FOUND));
    }
}
