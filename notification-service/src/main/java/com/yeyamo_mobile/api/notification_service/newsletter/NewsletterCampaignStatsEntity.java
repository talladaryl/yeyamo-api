package com.yeyamo_mobile.api.notification_service.newsletter;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="newsletter_campaign_stats") public class NewsletterCampaignStatsEntity {
 @Id @Column(name="campaign_id")public UUID campaignId;public long delivered;public long failed;public long opened;public long clicked;public long unsubscribed;@Column(name="updated_at",nullable=false)public Instant updatedAt;
}
