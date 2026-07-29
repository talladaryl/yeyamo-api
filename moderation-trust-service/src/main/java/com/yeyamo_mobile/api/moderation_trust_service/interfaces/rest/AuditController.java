package com.yeyamo_mobile.api.moderation_trust_service.interfaces.rest;

import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.moderation_trust_service.application.TrustSafetyAdminService;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.audit.AuditEntryEntity;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/moderation/audit")
@Tag(name="Moderation audit")
@SecurityRequirement(name="bearerAuth")
public class AuditController {
    private final TrustSafetyAdminService service;
    public AuditController(TrustSafetyAdminService service){this.service=service;}
    @GetMapping
    public Page<AuditEntryEntity> list(@RequestParam(required=false)String actorId,@RequestParam(required=false)String action,@RequestParam(required=false)String targetType,@RequestParam(required=false)String targetId,@RequestParam(required=false)String correlationId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdTo,Pageable pageable){
        return service.audits(actorId,action,targetType,targetId,correlationId,createdFrom,createdTo,pageable);
    }
}
