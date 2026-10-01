package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryCreateIdempotencyRepository extends JpaRepository<StoryCreateIdempotencyEntity, UUID> {
    Optional<StoryCreateIdempotencyEntity> findByAuthorIdAndIdempotencyKey(String authorId, String idempotencyKey);
}
