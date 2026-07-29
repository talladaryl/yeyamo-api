package com.yeyamo_mobile.api.support_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="support_internal_notes") public class SupportInternalNoteEntity {
 @Id public UUID id; @Column(name="conversation_id",nullable=false) public UUID conversationId;
 @Column(name="author_admin_id",nullable=false) public String authorAdminId; @Column(nullable=false,columnDefinition="TEXT") public String content;
 @Column(name="created_at",nullable=false) public Instant createdAt;
}
