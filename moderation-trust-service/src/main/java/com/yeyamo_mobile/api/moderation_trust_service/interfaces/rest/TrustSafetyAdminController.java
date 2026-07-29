package com.yeyamo_mobile.api.moderation_trust_service.interfaces.rest;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.*;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.moderation_trust_service.application.*;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.audit.AuditEntryEntity;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
public class TrustSafetyAdminController {
    private final TrustSafetyAdminService admin;
    public TrustSafetyAdminController(TrustSafetyAdminService admin){this.admin=admin;}

    @GetMapping("/api/v1/moderation/admin/reports")
    public Page<ModerationReportEntity> reports(@RequestParam(required=false)ReportStatus status,@RequestParam(required=false)ReportReason reason,@RequestParam(required=false)TargetType targetType,@RequestParam(required=false)String targetId,@RequestParam(required=false)String reporterId,@RequestParam(required=false)String assigneeId,@RequestParam(required=false)String priority,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdTo,Pageable pageable,Authentication auth){
        requireModerator(auth);return admin.reports(status,reason,targetType,targetId,reporterId,assigneeId,priority,createdFrom,createdTo,pageable);
    }
    @PostMapping("/api/v1/moderation/reports/{id}/assign")
    public ModerationReportEntity assign(@PathVariable UUID id,@Valid@RequestBody AssignRequest body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
        requireModerator(auth);return admin.assign(id,body.assignSelf()?auth.getName():body.assigneeId(),auth.getName(),correlation);
    }
    @GetMapping("/api/v1/trust")
    public Page<TrustScoreEntity> trust(Pageable pageable,Authentication auth){requireModerator(auth);return admin.trust(pageable);}
    @GetMapping("/api/v1/trust/{subjectId}/history")
    public Page<SanctionEntity> history(@PathVariable String subjectId,Pageable pageable,Authentication auth){requireModerator(auth);return admin.history(subjectId,pageable);}
    @PostMapping("/api/v1/trust/{subjectId}/sanctions")
    public SanctionEntity sanction(@PathVariable String subjectId,@Valid@RequestBody SanctionRequest body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
        requireModerator(auth);return admin.sanction(subjectId,body.type(),body.reason(),body.startAt(),body.endAt(),body.reportId(),auth.getName(),correlation);
    }
    @GetMapping("/api/v1/moderation/admin/audit")
    public Page<AuditEntryEntity> audit(@RequestParam(required=false)String actorId,@RequestParam(required=false)String action,@RequestParam(required=false)String targetType,@RequestParam(required=false)String targetId,@RequestParam(required=false)String correlationId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdTo,Pageable pageable,Authentication auth){
        requireModerator(auth);return admin.audits(actorId,action,targetType,targetId,correlationId,createdFrom,createdTo,pageable);
    }
    private void requireModerator(Authentication auth){
        boolean allowed=auth!=null&&auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(value->value.equals("ROLE_ADMIN")||value.equals("ROLE_SUPER_ADMIN")||value.equals("ROLE_MODERATOR"));
        if(!allowed)throw new ModerationException("TRUST_FORBIDDEN","Moderator permission is required");
    }
    public record AssignRequest(String assigneeId,boolean assignSelf){}
    public record SanctionRequest(@NotNull SanctionType type,@NotBlank@Size(max=2000)String reason,Instant startAt,Instant endAt,UUID reportId){}
}
