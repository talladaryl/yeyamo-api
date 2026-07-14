package com.yeyamo_mobile.api.admin_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.admin_service.dto.ReportRequest;
import com.yeyamo_mobile.api.admin_service.dto.ReportResolutionRequest;
import com.yeyamo_mobile.api.admin_service.enums.ReportStatus;
import com.yeyamo_mobile.api.admin_service.exception.ApiException;
import com.yeyamo_mobile.api.admin_service.models.Report;
import com.yeyamo_mobile.api.admin_service.repository.ReportRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ReportService {

    private final ReportRepository repository;
    private final AuditService auditService;

    public ReportService(ReportRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Report> list(ReportStatus status) {
        return status == null ? repository.findAllByOrderByCreatedAtDesc() : repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public Report create(ReportRequest request) {
        Report report = new Report();
        report.setReportType(request.reportType());
        report.setTargetId(request.targetId());
        report.setReporterId(request.reporterId());
        report.setReason(request.reason());
        report.setDescription(request.description());
        return repository.save(report);
    }

    @Transactional
    public Report resolve(UUID id, ReportResolutionRequest request, HttpServletRequest httpRequest) {
        Report report = get(id);
        report.setStatus(request.status());
        report.setAssignedTo(request.assignedTo());
        report.setResolutionComment(request.resolutionComment());
        if (request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.REJECTED) {
            report.setResolvedBy(request.resolvedBy() == null ? auditService.currentAdminId() : request.resolvedBy());
            report.setResolvedAt(LocalDateTime.now());
        }
        Report saved = repository.save(report);
        auditService.record("RESOLVE_REPORT", "REPORT", saved.getId(), Map.of("status", saved.getStatus().name()), httpRequest);
        return saved;
    }

    @Transactional(readOnly = true)
    public Report get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException("REPORT_NOT_FOUND", "Signalement introuvable", HttpStatus.NOT_FOUND));
    }
}
