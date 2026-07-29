package com.yeyamo_mobile.api.gamification_service.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.gamification_service.infrastructure.persistence.XpLedgerEntity;
import com.yeyamo_mobile.api.gamification_service.infrastructure.persistence.XpLedgerRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/gamification")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Gamification administration")
@SecurityRequirement(name = "bearerAuth")
public class GamificationAdminController {
    private final XpLedgerRepository ledger;

    public GamificationAdminController(XpLedgerRepository ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/xp-ledger")
    @Operation(
            summary = "List append-only XP ledger entries",
            description = "Requires ADMIN or SUPER_ADMIN. Supports optional user filtering and server pagination.")
    public Page<XpLedgerResponse> ledger(
            @RequestParam(required = false) String userId,
            @PageableDefault(size = 25, sort = "createdAt") Pageable pageable) {
        Page<XpLedgerEntity> page = userId == null || userId.isBlank()
                ? ledger.findAll(pageable)
                : ledger.findByUserId(userId, pageable);
        return page.map(XpLedgerResponse::from);
    }

    public record XpLedgerResponse(
            UUID id,
            UUID eventId,
            String userId,
            int xpDelta,
            String event,
            String reference,
            Instant occurredAt,
            Instant createdAt) {
        static XpLedgerResponse from(XpLedgerEntity entity) {
            return new XpLedgerResponse(
                    entity.getId(),
                    entity.getEventId(),
                    entity.getUserId(),
                    entity.getPoints(),
                    entity.getReason(),
                    entity.getSourceId(),
                    entity.getOccurredAt(),
                    entity.getCreatedAt());
        }
    }
}
