package com.yeyamo_mobile.api.moderation_trust_service.interfaces.rest;

import java.time.Instant;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        ReportStatus status,
        ReportStatus decision,
        @Size(max=2000) String resolution,
        @Size(max=2000) String reason,
        @Valid SanctionRequest sanction) {
    public ReportStatus effectiveDecision(){return decision!=null?decision:status;}
    public String effectiveReason(){return reason!=null&&!reason.isBlank()?reason:resolution;}
    public record SanctionRequest(SanctionType type,String subjectId,Instant startAt,Instant endAt){}
}
