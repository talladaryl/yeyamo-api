package com.yeyamo_mobile.api.notification_service.newsletter;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="newsletter_campaigns") public class NewsletterCampaignEntity {
 @Id public UUID id;@Column(nullable=false)public String name;@Column(nullable=false)public String subject;public String preheader;
 @Column(nullable=false,columnDefinition="TEXT")public String content;@Enumerated(EnumType.STRING)@Column(nullable=false)public NewsletterStatus status;
 @Column(name="segment_json",nullable=false,columnDefinition="TEXT")public String segmentJson;@Column(name="scheduled_at")public Instant scheduledAt;
 @Column(name="created_by",nullable=false)public String createdBy;@Column(name="created_at",nullable=false)public Instant createdAt;@Column(name="updated_at",nullable=false)public Instant updatedAt;
 @Column(name="dispatch_key")public UUID dispatchKey;@Version public long version;
}
