package com.yeyamo_mobile.api.admin_service.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.admin_service.dto.ModerationActionRequest;
import com.yeyamo_mobile.api.admin_service.enums.ModerationTargetType;
import com.yeyamo_mobile.api.admin_service.models.ModerationAction;
import com.yeyamo_mobile.api.admin_service.repository.ModerationActionRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ModerationService {

    private final ModerationActionRepository repository;
    private final AuditService auditService;

    public ModerationService(ModerationActionRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ModerationAction> history(ModerationTargetType targetType, UUID targetId) {
        return repository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }

    @Transactional
    public ModerationAction apply(ModerationActionRequest request, HttpServletRequest httpRequest) {
        ModerationAction action = new ModerationAction();
        action.setAdminId(auditService.currentAdminId());
        action.setActionType(request.actionType());
        action.setTargetType(request.targetType());
        action.setTargetId(request.targetId());
        action.setPreviousStatus(request.previousStatus());
        action.setNewStatus(request.newStatus());
        action.setReason(request.reason());
        action.setDetails(request.safeDetails());
        ModerationAction saved = repository.save(action);
        auditService.record("MODERATE_" + request.targetType().name(), request.targetType().name(), request.targetId(), Map.of(
                "actionType", request.actionType().name(),
                "newStatus", request.newStatus()
        ), httpRequest);
        return saved;
    }
}
