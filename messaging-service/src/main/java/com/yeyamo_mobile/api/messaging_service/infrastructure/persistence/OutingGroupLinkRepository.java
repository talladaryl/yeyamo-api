package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutingGroupLinkRepository extends JpaRepository<OutingGroupLinkEntity, UUID> { }
