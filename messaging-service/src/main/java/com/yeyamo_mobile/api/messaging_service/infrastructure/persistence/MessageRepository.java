package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yeyamo_mobile.api.messaging_service.domain.MemberStatus;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Slice<MessageEntity> findByConversationIdOrderBySentAtDescIdDesc(UUID conversationId, Pageable pageable);
    @Query("select m from MessageEntity m where m.conversationId = :conversationId and (m.sentAt < :before or (:beforeId is not null and m.sentAt = :before and m.id < :beforeId)) order by m.sentAt desc, m.id desc")
    Slice<MessageEntity> findByConversationIdBeforeCursor(@Param("conversationId") UUID conversationId, @Param("before") Instant before, @Param("beforeId") UUID beforeId, Pageable pageable);
    @Query("select m.conversationId, count(m) from MessageEntity m, ConversationMemberEntity member where member.conversationId = m.conversationId and member.userId = :viewerId and member.status = :status and m.conversationId in :conversationIds and m.senderId <> :viewerId and (member.lastReadAt is null or m.sentAt > member.lastReadAt) and m.deletedAt is null group by m.conversationId")
    List<Object[]> countUnreadForViewer(@Param("viewerId") String viewerId, @Param("status") MemberStatus status, @Param("conversationIds") List<UUID> conversationIds);
}
