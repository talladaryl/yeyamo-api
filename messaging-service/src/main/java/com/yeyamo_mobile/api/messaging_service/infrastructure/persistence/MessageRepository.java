package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Slice<MessageEntity> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);
    Slice<MessageEntity> findByConversationIdAndSentAtLessThanOrderBySentAtDesc(UUID conversationId, Instant before, Pageable pageable);
}
