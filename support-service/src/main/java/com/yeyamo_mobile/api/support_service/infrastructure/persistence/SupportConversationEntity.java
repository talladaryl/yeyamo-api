package com.yeyamo_mobile.api.support_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.support_service.domain.*;
import jakarta.persistence.*;

@Entity @Table(name="support_conversations")
public class SupportConversationEntity {
    @Id public UUID id;
    @Column(name="user_id",nullable=false) public String userId;
    @Column(nullable=false) public String subject;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public SupportStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public SupportPriority priority;
    @Column(name="assignee_admin_id") public String assigneeAdminId;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="updated_at",nullable=false) public Instant updatedAt;
    @Column(name="first_response_at") public Instant firstResponseAt;
    @Column(name="resolved_at") public Instant resolvedAt;
    @Version public long version;
}
