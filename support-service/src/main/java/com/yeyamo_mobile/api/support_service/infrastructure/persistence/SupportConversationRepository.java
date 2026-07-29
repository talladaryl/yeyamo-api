package com.yeyamo_mobile.api.support_service.infrastructure.persistence;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface SupportConversationRepository extends JpaRepository<SupportConversationEntity,UUID>,JpaSpecificationExecutor<SupportConversationEntity>{}
