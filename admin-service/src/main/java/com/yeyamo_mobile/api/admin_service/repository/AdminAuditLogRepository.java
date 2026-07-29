package com.yeyamo_mobile.api.admin_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.yeyamo_mobile.api.admin_service.models.AdminAuditLog;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID>, JpaSpecificationExecutor<AdminAuditLog> {
    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();
}
