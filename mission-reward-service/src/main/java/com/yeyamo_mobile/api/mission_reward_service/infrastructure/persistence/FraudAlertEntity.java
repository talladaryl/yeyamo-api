package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="mission_fraud_alerts")public class FraudAlertEntity{
 @Id private UUID id;@Column(name="user_id",nullable=false,length=120)private String userId;@Column(nullable=false,length=120)private String rule;@Column(nullable=false,length=20)private String severity;@Column(nullable=false,length=20)private String status;@Column(name="evidence_json",nullable=false,columnDefinition="TEXT")private String evidence;@Column(name="source_reference",nullable=false,unique=true,length=180)private String sourceReference;@Column(name="created_at",nullable=false)private Instant createdAt;
 public UUID getId(){return id;}public String getUserId(){return userId;}public String getRule(){return rule;}public String getSeverity(){return severity;}public String getStatus(){return status;}public String getEvidence(){return evidence;}public String getSourceReference(){return sourceReference;}public Instant getCreatedAt(){return createdAt;}
}
