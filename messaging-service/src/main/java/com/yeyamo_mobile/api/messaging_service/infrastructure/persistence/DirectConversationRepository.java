package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectConversationRepository extends JpaRepository<DirectConversationEntity, String> {
}
