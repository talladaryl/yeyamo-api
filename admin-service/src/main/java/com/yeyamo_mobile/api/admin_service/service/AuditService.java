package com.yeyamo_mobile.api.admin_service.service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.admin_service.models.AdminAuditLog;
import com.yeyamo_mobile.api.admin_service.repository.AdminAuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditService {

    private final AdminAuditLogRepository auditLogRepository;

    public AuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String action, String targetType, UUID targetId, Map<String, Object> details, HttpServletRequest request) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(currentAdminId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details == null ? new LinkedHashMap<>() : details);
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setCorrelationId(correlationId(request));
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> list() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public UUID currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication == null ? "anonymous" : authentication.getName();
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException exception) {
            return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        }
    }

    private UUID correlationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        if (header == null || header.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException exception) {
            return UUID.nameUUIDFromBytes(header.getBytes(StandardCharsets.UTF_8));
        }
    }
}
