package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.ConversationSummary;
import com.yeyamo_mobile.api.messaging_service.domain.MemberStatus;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, ConversationMemberId> {
    List<ConversationMemberEntity> findByConversationId(UUID conversationId);
    Optional<ConversationMemberEntity> findByConversationIdAndUserId(UUID conversationId, String userId);
    List<ConversationMemberEntity> findByUserIdAndStatus(String userId, MemberStatus status);

    @Query("SELECT new com.yeyamo_mobile.api.messaging_service.application.MessagingDtos$ConversationSummary(c.id, c.type, c.title, m.role, c.updatedAt, c.lastMessagePreview, c.lastMessageAt) " +
           "FROM ConversationMemberEntity m, ConversationEntity c " +
           "WHERE m.conversationId = c.id AND m.userId = :userId AND m.status = :status " +
           "ORDER BY c.updatedAt DESC")
    List<ConversationSummary> findUserConversationSummaries(@Param("userId") String userId, @Param("status") MemberStatus status);
}
