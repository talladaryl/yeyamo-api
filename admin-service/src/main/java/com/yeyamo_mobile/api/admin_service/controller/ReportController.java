package com.yeyamo_mobile.api.admin_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.admin_service.dto.ReportRequest;
import com.yeyamo_mobile.api.admin_service.dto.ReportResolutionRequest;
import com.yeyamo_mobile.api.admin_service.enums.ReportStatus;
import com.yeyamo_mobile.api.admin_service.models.Report;
import com.yeyamo_mobile.api.admin_service.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Validated
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public List<Report> list(@RequestParam(required = false) ReportStatus status) {
        return service.list(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Report create(@Valid @RequestBody ReportRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/resolution")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public Report resolve(@PathVariable UUID id, @Valid @RequestBody ReportResolutionRequest request, HttpServletRequest httpRequest) {
        return service.resolve(id, request, httpRequest);
    }
}
