package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentModerationAuditRepository extends JpaRepository<ContentModerationAudit,UUID>{List<ContentModerationAudit>findByContentTypeAndContentIdOrderByCreatedAtDesc(String type,UUID id);}
