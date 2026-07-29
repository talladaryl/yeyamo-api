package com.yeyamo_mobile.api.admin_service.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.admin_service.dto.AdminPermissionResponse;
import com.yeyamo_mobile.api.admin_service.dto.AdminUserResponse;
import com.yeyamo_mobile.api.admin_service.dto.CreateAdminUserRequest;
import com.yeyamo_mobile.api.admin_service.dto.UpdateAdminUserRequest;
import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;
import com.yeyamo_mobile.api.admin_service.exception.ApiException;
import com.yeyamo_mobile.api.admin_service.models.AdminUser;
import com.yeyamo_mobile.api.admin_service.repository.AdminUserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AdminUserService {
    private static final Set<String> ALLOWED_PERMISSIONS = Set.of(
            "admin:read", "admin:manage", "users:read", "users:suspend", "users:roles",
            "partners:review", "places:review", "moderation:decide", "support:manage",
            "finance:read", "finance:refund", "finance:adjust", "campaign:read",
            "campaign:approve", "campaign:reject", "analytics:read", "audit:read");
    private static final Set<String> ALLOWED_SCOPES = Set.of(
            "campaign:read", "campaign:approve", "campaign:reject", "campaign:create",
            "campaign:update", "campaign:submit", "campaign:pause");

    private final AdminUserRepository repository;
    private final AuditService auditService;

    public AdminUserService(AdminUserRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() { return repository.findAll().stream().map(this::response).toList(); }

    @Transactional
    public AdminUserResponse create(CreateAdminUserRequest request, HttpServletRequest httpRequest) {
        validateUnique(request.userId(), request.email(), null);
        Set<String> permissions = validatePermissions(request.permissions());
        Set<String> scopes = validateScopes(request.scopes());
        AdminUser admin = new AdminUser();
        admin.setUserId(request.userId());
        admin.setName(request.name().trim());
        admin.setEmail(normalizeEmail(request.email()));
        admin.setRole(request.role());
        admin.setPermissions(permissionMap(permissions));
        admin.setScopes(scopes);
        admin.setStatus(request.status() == null ? AdminStatus.ACTIVE : request.status());
        admin.setCreatedBy(auditService.currentAdminId());
        AdminUser saved = repository.save(admin);
        auditService.record("ADMIN_CREATED", "ADMIN_USER", saved.getId(), safeDetails(saved), httpRequest);
        return response(saved);
    }

    @Transactional
    public AdminUserResponse update(UUID id, UpdateAdminUserRequest request, HttpServletRequest httpRequest) {
        AdminUser admin = entity(id);
        validateUnique(admin.getUserId(), request.email(), id);
        preventSelfDisable(admin, request.status());
        preventLastSuperAdmin(admin, request.role(), request.status());

        AdminRole previousRole = admin.getRole();
        AdminStatus previousStatus = admin.getStatus();
        Set<String> previousPermissions = enabledPermissions(admin);
        Set<String> permissions = validatePermissions(request.permissions());
        Set<String> scopes = validateScopes(request.scopes());

        admin.setName(request.name().trim());
        admin.setEmail(normalizeEmail(request.email()));
        admin.setRole(request.role());
        admin.setPermissions(permissionMap(permissions));
        admin.setScopes(scopes);
        admin.setStatus(request.status());
        AdminUser saved = repository.save(admin);

        auditService.record("ADMIN_UPDATED", "ADMIN_USER", id, safeDetails(saved), httpRequest);
        if (previousRole != saved.getRole()) auditService.record("ADMIN_ROLE_CHANGED", "ADMIN_USER", id,
                Map.of("from", previousRole.name(), "to", saved.getRole().name()), httpRequest);
        if (!previousPermissions.equals(permissions)) auditService.record("ADMIN_PERMISSION_CHANGED", "ADMIN_USER", id,
                Map.of("permissions", permissions), httpRequest);
        if (previousStatus != AdminStatus.DISABLED && saved.getStatus() == AdminStatus.DISABLED)
            auditService.record("ADMIN_DISABLED", "ADMIN_USER", id, Map.of(), httpRequest);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) { return response(entity(id)); }

    private AdminUser entity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException("ADMIN_USER_NOT_FOUND",
                "Administrateur introuvable", HttpStatus.NOT_FOUND));
    }

    private void validateUnique(UUID userId, String email, UUID currentId) {
        if (currentId == null && repository.existsByUserId(userId))
            throw conflict("ADMIN_USER_EXISTS", "Cet utilisateur est deja administrateur");
        boolean duplicateEmail = currentId == null ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (duplicateEmail) throw conflict("ADMIN_EMAIL_EXISTS", "Cet email administrateur existe deja");
    }

    private void preventSelfDisable(AdminUser admin, AdminStatus status) {
        if (admin.getId().equals(auditService.currentAdminId()) && status != AdminStatus.ACTIVE)
            throw conflict("ADMIN_SELF_DISABLE_FORBIDDEN", "Un administrateur ne peut pas desactiver son propre compte");
    }

    private void preventLastSuperAdmin(AdminUser admin, AdminRole nextRole, AdminStatus nextStatus) {
        boolean removesActiveSuperAdmin = admin.getRole() == AdminRole.SUPER_ADMIN && admin.getStatus() == AdminStatus.ACTIVE
                && (nextRole != AdminRole.SUPER_ADMIN || nextStatus != AdminStatus.ACTIVE);
        if (removesActiveSuperAdmin && repository.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminStatus.ACTIVE) <= 1)
            throw conflict("LAST_SUPER_ADMIN_REQUIRED", "Le dernier SUPER_ADMIN actif ne peut pas etre desactive");
    }

    private Set<String> validatePermissions(Set<String> requested) {
        Set<String> values = requested == null ? Set.of() : Set.copyOf(requested);
        if (!ALLOWED_PERMISSIONS.containsAll(values))
            throw new ApiException("INVALID_ADMIN_PERMISSION", "Permission administrateur inconnue", HttpStatus.BAD_REQUEST);
        return values;
    }

    private Set<String> validateScopes(Set<String> requested) {
        Set<String> values = requested == null ? Set.of() : Set.copyOf(requested);
        if (!ALLOWED_SCOPES.containsAll(values))
            throw new ApiException("INVALID_ADMIN_SCOPE", "Scope administrateur inconnu", HttpStatus.BAD_REQUEST);
        return values;
    }

    private Map<String, Object> permissionMap(Set<String> permissions) {
        Map<String, Object> result = new LinkedHashMap<>();
        permissions.forEach(permission -> result.put(permission, true));
        return result;
    }

    private Set<String> enabledPermissions(AdminUser admin) {
        return admin.getPermissions().entrySet().stream().filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private AdminUserResponse response(AdminUser admin) {
        Set<AdminPermissionResponse> permissions = admin.getPermissions().entrySet().stream()
                .map(entry -> new AdminPermissionResponse(entry.getKey(), Boolean.TRUE.equals(entry.getValue())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new AdminUserResponse(admin.getId(), admin.getUserId(), admin.getName(), admin.getEmail(),
                Set.of(admin.getRole()), permissions, Set.copyOf(admin.getScopes()), admin.getStatus(),
                admin.getCreatedAt(), admin.getUpdatedAt(), admin.getLastLoginAt());
    }

    private Map<String, Object> safeDetails(AdminUser admin) {
        return Map.of("role", admin.getRole().name(), "status", admin.getStatus().name(),
                "permissions", enabledPermissions(admin), "scopes", admin.getScopes());
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(); }
    private ApiException conflict(String code, String message) { return new ApiException(code, message, HttpStatus.CONFLICT); }
}
