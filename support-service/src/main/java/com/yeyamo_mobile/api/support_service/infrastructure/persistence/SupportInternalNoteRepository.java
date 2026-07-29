package com.yeyamo_mobile.api.support_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface SupportInternalNoteRepository extends JpaRepository<SupportInternalNoteEntity,UUID>{@Query("select n from SupportInternalNoteEntity n where n.conversationId=:id order by n.createdAt")List<SupportInternalNoteEntity> findForConversation(@Param("id")UUID id);}
