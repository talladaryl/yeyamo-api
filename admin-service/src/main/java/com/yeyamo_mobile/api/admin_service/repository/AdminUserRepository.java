package com.yeyamo_mobile.api.admin_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.admin_service.models.AdminUser;
import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    boolean existsByUserId(UUID userId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    long countByRoleAndStatus(AdminRole role, AdminStatus status);
}
