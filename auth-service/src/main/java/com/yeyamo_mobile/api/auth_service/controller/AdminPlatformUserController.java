package com.yeyamo_mobile.api.auth_service.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.yeyamo_mobile.api.auth_service.dto.AdminPlatformUserDetail;
import com.yeyamo_mobile.api.auth_service.dto.AdminPlatformUserSummary;
import com.yeyamo_mobile.api.auth_service.dto.AdminRevokeSessionsRequest;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserRolesRequest;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserSessionResponse;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserStatusRequest;
import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.service.AdminPlatformUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/platform-users")
public class AdminPlatformUserController {
    private final AdminPlatformUserService service;

    public AdminPlatformUserController(AdminPlatformUserService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')")
    public Page<AdminPlatformUserSummary> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) String email, @RequestParam(required = false) String phone,
            @RequestParam(required = false) UserStatus status, @RequestParam(required = false) Roles role,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastLoginFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastLoginTo,
            Pageable pageable) {
        return service.list(search, email, phone, status, role, regionId, createdFrom, createdTo, lastLoginFrom,
                lastLoginTo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')")
    public AdminPlatformUserDetail detail(@PathVariable Long id) { return service.detail(id); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public AdminPlatformUserDetail status(@PathVariable Long id, @Valid @RequestBody AdminUserStatusRequest request,
            Authentication actor, @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return service.changeStatus(id, request, actor, correlationId);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminPlatformUserDetail roles(@PathVariable Long id, @Valid @RequestBody AdminUserRolesRequest request,
            Authentication actor, @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return service.changeRoles(id, request, actor, correlationId);
    }

    @GetMapping("/{id}/sessions")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')")
    public List<AdminUserSessionResponse> sessions(@PathVariable Long id) { return service.sessions(id); }

    @PostMapping("/{id}/sessions/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> revoke(@PathVariable Long id, @Valid @RequestBody AdminRevokeSessionsRequest request,
            Authentication actor, @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        service.revokeSessions(id, request, actor, correlationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StreamingResponseBody> export(@RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status, @RequestParam(required = false) Roles role,
            @RequestParam(required = false) Long regionId, Authentication actor,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        StreamingResponseBody body = output -> service.export(output, search, status, role, regionId,
                actor.getName(), correlationId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=yeyamo-platform-users.csv").body(body);
    }
}
