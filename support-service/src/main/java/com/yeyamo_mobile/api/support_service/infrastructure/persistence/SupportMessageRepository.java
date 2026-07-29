package com.yeyamo_mobile.api.support_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity,UUID>{@Query("select m from SupportMessageEntity m where m.conversationId=:id order by m.createdAt")List<SupportMessageEntity> findForConversation(@Param("id")UUID id);}
