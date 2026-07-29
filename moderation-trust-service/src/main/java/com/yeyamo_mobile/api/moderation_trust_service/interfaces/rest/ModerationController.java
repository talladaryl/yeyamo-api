package com.yeyamo_mobile.api.moderation_trust_service.interfaces.rest;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.moderation_trust_service.application.*;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/v1/moderation/reports")
@Tag(name="Moderation reports")
public class ModerationController {
    private final ModerationService service;
    private final TrustSafetyAdminService admin;
    public ModerationController(ModerationService service,TrustSafetyAdminService admin){this.service=service;this.admin=admin;}

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid@RequestBody ReportRequest request,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
        return ReportResponse.from(service.report(request.targetType(),request.targetId(),request.targetOwnerId(),auth.getName(),request.reason(),request.details(),correlation));
    }
    @GetMapping("/me")
    public List<ReportResponse> mine(@RequestParam(defaultValue="50")@Min(1)@Max(200)int limit,Authentication auth){
        return service.list(null,auth.getName(),limit).stream().map(ReportResponse::from).toList();
    }
    @GetMapping
    public Page<ReportResponse> list(@RequestParam(required=false)ReportStatus status,@RequestParam(required=false)ReportReason reason,@RequestParam(required=false)TargetType targetType,@RequestParam(required=false)String targetId,@RequestParam(required=false)String reporterId,@RequestParam(required=false)String assigneeId,@RequestParam(required=false)String priority,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdTo,Pageable pageable){
        return admin.reports(status,reason,targetType,targetId,reporterId,assigneeId,priority,createdFrom,createdTo,pageable).map(entity->ReportResponse.from(toDomain(entity)));
    }
    @GetMapping("/{id}") public ReportResponse get(@PathVariable UUID id){return ReportResponse.from(service.get(id));}
    @PostMapping("/{id}/review")
    public ReportResponse review(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return ReportResponse.from(service.review(id,auth.getName(),correlation));}
    @PostMapping("/{id}/decision")
    public ReportResponse decide(@PathVariable UUID id,@Valid@RequestBody DecisionRequest request,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
        if(request.effectiveDecision()==null||request.effectiveReason()==null||request.effectiveReason().isBlank())throw new IllegalArgumentException("decision and reason are required");
        ReportResponse response=ReportResponse.from(service.decide(id,request.effectiveDecision(),auth.getName(),request.effectiveReason(),correlation));
        if(request.sanction()!=null){
            String subject=request.sanction().subjectId()!=null?request.sanction().subjectId():response.targetOwnerId();
            admin.sanction(subject,request.sanction().type(),request.effectiveReason(),request.sanction().startAt(),request.sanction().endAt(),id,auth.getName(),correlation);
        }
        return response;
    }
    private ModerationReport toDomain(com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence.ModerationReportEntity entity){
        ModerationReport report=new ModerationReport();report.setId(entity.getId());report.setTargetType(entity.getTargetType());report.setTargetId(entity.getTargetId());report.setTargetOwnerId(entity.getTargetOwnerId());report.setReporterId(entity.getReporterId());report.setReason(entity.getReason());report.setDetails(entity.getDetails());report.setStatus(entity.getStatus());report.setAssignedTo(entity.getAssignedTo());report.setResolution(entity.getResolution());report.setCreatedAt(entity.getCreatedAt());report.setUpdatedAt(entity.getUpdatedAt());report.setDecidedAt(entity.getDecidedAt());report.setVersion(entity.getVersion());return report;
    }
}
