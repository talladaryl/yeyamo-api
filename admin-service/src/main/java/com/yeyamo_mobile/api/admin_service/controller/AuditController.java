package com.yeyamo_mobile.api.admin_service.controller;

import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;

import com.yeyamo_mobile.api.admin_service.models.AdminAuditLog;
import com.yeyamo_mobile.api.admin_service.service.AuditService;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<AdminAuditLog> list(
            @RequestParam(required=false)String actorId,@RequestParam(required=false)String action,
            @RequestParam(required=false)String targetType,@RequestParam(required=false)String targetId,
            @RequestParam(required=false)String serviceName,@RequestParam(required=false)String correlationId,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime createdFrom,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime createdTo,
            @PageableDefault(size=25,sort="createdAt",direction=Sort.Direction.DESC)Pageable pageable) {
        return service.search(actorId,action,targetType,targetId,serviceName,correlationId,createdFrom,createdTo,pageable);
    }
}
