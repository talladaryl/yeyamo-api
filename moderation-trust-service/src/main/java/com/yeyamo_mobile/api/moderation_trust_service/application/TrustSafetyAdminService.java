package com.yeyamo_mobile.api.moderation_trust_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.moderation_trust_service.application.port.*;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.audit.*;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence.*;

@Service
public class TrustSafetyAdminService {
    private final SpringModerationReportRepository reports; private final SpringTrustScoreRepository scores;
    private final SpringAuditRepository audits; private final SanctionRepository sanctions;
    private final ModerationAuditPort auditPort; private final ModerationOutboxPort outbox; private final ObjectMapper mapper;

    public TrustSafetyAdminService(SpringModerationReportRepository reports,SpringTrustScoreRepository scores,SpringAuditRepository audits,SanctionRepository sanctions,ModerationAuditPort auditPort,ModerationOutboxPort outbox,ObjectMapper mapper){
        this.reports=reports;this.scores=scores;this.audits=audits;this.sanctions=sanctions;this.auditPort=auditPort;this.outbox=outbox;this.mapper=mapper;
    }
    @Transactional(readOnly=true)
    public Page<ModerationReportEntity> reports(ReportStatus status,ReportReason reason,TargetType targetType,String targetId,String reporterId,String assigneeId,String priority,Instant from,Instant to,Pageable pageable){
        Specification<ModerationReportEntity> spec=(root,query,builder)->builder.conjunction();
        if(status!=null)spec=spec.and((r,q,b)->b.equal(r.get("status"),status)); if(reason!=null)spec=spec.and((r,q,b)->b.equal(r.get("reason"),reason));
        if(targetType!=null)spec=spec.and((r,q,b)->b.equal(r.get("targetType"),targetType)); if(has(targetId))spec=spec.and((r,q,b)->b.equal(r.get("targetId"),targetId));
        if(has(reporterId))spec=spec.and((r,q,b)->b.equal(r.get("reporterId"),reporterId)); if(has(assigneeId))spec=spec.and((r,q,b)->b.equal(r.get("assignedTo"),assigneeId));
        if(has(priority))spec=spec.and((r,q,b)->b.equal(r.get("priority"),priority.toUpperCase(Locale.ROOT)));
        if(from!=null)spec=spec.and((r,q,b)->b.greaterThanOrEqualTo(r.get("createdAt"),from)); if(to!=null)spec=spec.and((r,q,b)->b.lessThanOrEqualTo(r.get("createdAt"),to));
        return reports.findAll(spec,pageable);
    }
    @Transactional public ModerationReportEntity assign(UUID id,String assignee,String actor,String correlation){
        ModerationReportEntity report=reports.findById(id).orElseThrow(()->new ModerationException("REPORT_NOT_FOUND","Moderation report not found"));
        if(report.getStatus()!=ReportStatus.OPEN&&report.getStatus()!=ReportStatus.REVIEW)throw new IllegalStateException("Only open reports can be assigned");
        report.setAssignedTo(has(assignee)?assignee:actor);report.setStatus(ReportStatus.REVIEW);report.setUpdatedAt(Instant.now());report=reports.save(report);
        record("REPORT_ASSIGNED",id.toString(),actor,correlation,Map.of("assigneeId",report.getAssignedTo()));
        outbox.append("moderation.report.assigned","moderation-report",id.toString(),actor,correlation,Map.of("reportId",id,"assigneeId",report.getAssignedTo()));return report;
    }
    @Transactional public SanctionEntity sanction(String subjectId,SanctionType type,String reason,Instant startAt,Instant endAt,UUID reportId,String actor,String correlation){
        SanctionEntity sanction=sanctions.save(SanctionEntity.create(subjectId,type,reason,startAt,endAt,actor,reportId));
        record("SANCTION_CREATED",subjectId,actor,correlation,Map.of("sanctionId",sanction.getId(),"type",type));
        outbox.append("trust.sanction.created","trust-sanction",sanction.getId().toString(),actor,correlation,Map.of("sanctionId",sanction.getId(),"subjectId",subjectId,"type",type));return sanction;
    }
    @Transactional(readOnly=true) public Page<TrustScoreEntity> trust(Pageable pageable){return scores.findAll(pageable);}
    @Transactional(readOnly=true) public Page<SanctionEntity> history(String subjectId,Pageable pageable){return sanctions.findBySubjectIdOrderByCreatedAtDesc(subjectId,pageable);}
    @Transactional(readOnly=true) public Page<AuditEntryEntity> audits(String actorId,String action,String targetType,String targetId,String correlationId,Instant from,Instant to,Pageable pageable){
        Specification<AuditEntryEntity> spec=(root,query,builder)->builder.conjunction();
        if(has(actorId))spec=spec.and((r,q,b)->b.equal(r.get("actorId"),actorId)); if(has(action))spec=spec.and((r,q,b)->b.equal(r.get("action"),action));
        if(has(targetType))spec=spec.and((r,q,b)->b.equal(r.get("aggregateType"),targetType)); if(has(targetId))spec=spec.and((r,q,b)->b.equal(r.get("aggregateId"),targetId));
        if(has(correlationId))spec=spec.and((r,q,b)->b.equal(r.get("correlationId"),correlationId));
        if(from!=null)spec=spec.and((r,q,b)->b.greaterThanOrEqualTo(r.get("occurredAt"),from)); if(to!=null)spec=spec.and((r,q,b)->b.lessThanOrEqualTo(r.get("occurredAt"),to));
        return audits.findAll(spec,pageable);
    }
    private void record(String action,String target,String actor,String correlation,Map<String,Object> details){
        try{auditPort.append(AuditEntry.create(action,"MODERATION",target,actor,correlation,mapper.writeValueAsString(details)));}
        catch(Exception exception){throw new IllegalStateException("Cannot serialize audit entry",exception);}
    }
    private static boolean has(String value){return value!=null&&!value.isBlank();}
}
