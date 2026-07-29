package com.yeyamo_mobile.api.support_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="support_messages") public class SupportMessageEntity {
 @Id public UUID id; @Column(name="conversation_id",nullable=false) public UUID conversationId;
 @Column(name="sender_type",nullable=false) public String senderType; @Column(name="sender_id",nullable=false) public String senderId;
 @Column(nullable=false,columnDefinition="TEXT") public String content; @Column(name="attachment_media_ids",nullable=false,columnDefinition="TEXT") public String attachmentMediaIds;
 @Column(name="created_at",nullable=false) public Instant createdAt;
}
