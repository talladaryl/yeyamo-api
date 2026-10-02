package com.yeyamo_mobile.api.notification_service.infrastructure.persistence;
import java.time.Instant;import jakarta.persistence.*;
@Entity @Table(name="reengagement_digest_state")
public class ReengagementStateEntity{@Id @Column(name="user_id",length=120)String userId;@Column(name="last_sent_at",nullable=false)Instant lastSentAt;protected ReengagementStateEntity(){}public ReengagementStateEntity(String userId,Instant sent){this.userId=userId;this.lastSentAt=sent;}public Instant getLastSentAt(){return lastSentAt;}public void setLastSentAt(Instant value){lastSentAt=value;}}
