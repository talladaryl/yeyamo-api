package com.yeyamo_mobile.api.admin_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.admin_service.dto.ModerationActionRequest;
import com.yeyamo_mobile.api.admin_service.enums.ModerationTargetType;
import com.yeyamo_mobile.api.admin_service.models.ModerationAction;
import com.yeyamo_mobile.api.admin_service.service.ModerationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/moderation-actions")
@Validated
public class ModerationController {

    private final ModerationService service;

    public ModerationController(ModerationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public List<ModerationAction> history(@RequestParam ModerationTargetType targetType, @RequestParam UUID targetId) {
        return service.history(targetType, targetId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public ModerationAction apply(@Valid @RequestBody ModerationActionRequest request, HttpServletRequest httpRequest) {
        return service.apply(request, httpRequest);
    }
}
