package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recommendation_feedback", uniqueConstraints = @UniqueConstraint(name = "uk_recommendation_feedback", columnNames = {"user_id", "target_type", "target_id"}))
public class RecommendationFeedbackEntity {
    @Id UUID id;
    @Column(name = "user_id", nullable = false, length = 120) String userId;
    @Column(name = "target_type", nullable = false, length = 40) String targetType;
    @Column(name = "target_id", nullable = false, length = 120) String targetId;
    @Column(name = "feedback_type", nullable = false, length = 20) String feedbackType;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    protected RecommendationFeedbackEntity() { }
    RecommendationFeedbackEntity(String userId, String targetType, String targetId, String feedbackType) { this.id = UUID.randomUUID(); this.userId = userId; this.targetType = targetType; this.targetId = targetId; this.feedbackType = feedbackType; this.updatedAt = Instant.now(); }
}
