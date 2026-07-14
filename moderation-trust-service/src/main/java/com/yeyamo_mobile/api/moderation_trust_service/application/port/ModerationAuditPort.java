package com.yeyamo_mobile.api.moderation_trust_service.application.port;import java.util.List;import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuditEntry;
public interface ModerationAuditPort{void append(AuditEntry e);List<AuditEntry>latest(int limit);}
