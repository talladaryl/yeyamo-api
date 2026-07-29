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
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import jakarta.persistence.criteria.Predicate;

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
        log.setActorId(currentAdminId().toString());
        log.setActorRole(currentRole());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetIdValue(targetId == null ? null : targetId.toString());
        log.setService("admin-service");
        log.setDetails(sanitize(details));
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setCorrelationId(correlationId(request));
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> list() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void recordExternal(String actorId, String actorRole, String action, String targetType,
            String targetId, String service, String correlationId, Map<String, Object> metadata) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(stableUuid(actorId));
        log.setActorId(actorId);
        log.setActorRole(actorRole);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : stableUuid(targetId));
        log.setTargetIdValue(targetId);
        log.setService(service);
        log.setCorrelationId(stableUuid(correlationId));
        log.setDetails(sanitize(metadata));
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> search(String actorId,String action,String targetType,String targetId,
            String service,String correlationId,LocalDateTime from,LocalDateTime to,Pageable pageable) {
        Specification<AdminAuditLog> specification=(root,query,cb)->{
            List<Predicate> values=new java.util.ArrayList<>();
            if(text(actorId)!=null)values.add(cb.equal(root.get("actorId"),actorId));
            if(text(action)!=null)values.add(cb.equal(root.get("action"),action));
            if(text(targetType)!=null)values.add(cb.equal(root.get("targetType"),targetType));
            if(text(targetId)!=null)values.add(cb.equal(root.get("targetIdValue"),targetId));
            if(text(service)!=null)values.add(cb.equal(root.get("service"),service));
            if(text(correlationId)!=null)values.add(cb.equal(root.get("correlationId"),stableUuid(correlationId)));
            if(from!=null)values.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));
            if(to!=null)values.add(cb.lessThanOrEqualTo(root.get("createdAt"),to));
            return cb.and(values.toArray(Predicate[]::new));
        };
        return auditLogRepository.findAll(specification,pageable);
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

    public String currentRole() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null)return "UNKNOWN";
        return authentication.getAuthorities().stream().map(a->a.getAuthority())
                .filter(a->a.startsWith("ROLE_")).findFirst().orElse("UNKNOWN").replace("ROLE_","");
    }

    @SuppressWarnings("unchecked")
    public Map<String,Object> sanitize(Map<String,Object> source) {
        Map<String,Object> clean=new LinkedHashMap<>();
        if(source==null)return clean;
        source.forEach((key,value)->{
            String normalized=key.toLowerCase(java.util.Locale.ROOT);
            if(normalized.matches(".*(password|token|secret|credential|authorization|card|cvv|private.?key).*"))return;
            if(value instanceof Map<?,?> nested)clean.put(key,sanitize((Map<String,Object>)nested));
            else if(value instanceof String text)clean.put(key,text.substring(0,Math.min(text.length(),2000)));
            else clean.put(key,value);
        });
        return clean;
    }

    private UUID stableUuid(String value){return value==null||value.isBlank()?UUID.randomUUID():UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));}
    private String text(String value){return value==null||value.isBlank()?null:value.trim();}

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
