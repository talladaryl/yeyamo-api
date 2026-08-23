package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageIdempotencyRepository extends JpaRepository<MessageIdempotencyEntity, MessageIdempotencyId> {
    Optional<MessageIdempotencyEntity> findBySenderIdAndClientMessageId(String senderId, String clientMessageId);
}
