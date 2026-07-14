package com.yeyamo_mobile.api.admin_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.admin_service.models.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    boolean existsByUserId(UUID userId);
}
