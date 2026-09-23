package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedbackEntity, UUID> {
    Optional<RecommendationFeedbackEntity> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
    List<RecommendationFeedbackEntity> findByUserIdAndTargetIdIn(String userId, Collection<String> targetIds);
}
