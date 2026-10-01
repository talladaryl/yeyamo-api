package com.yeyamo_mobile.api.event_service.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yeyamo_mobile.api.event_service.models.EventCreateIdempotency;

public interface EventCreateIdempotencyRepository extends JpaRepository<EventCreateIdempotency, UUID> {
    Optional<EventCreateIdempotency> findByOwnerUserIdAndIdempotencyKey(String ownerUserId, String idempotencyKey);
}
