package com.yeyamo_mobile.api.partner_service.infrastructure.messaging;
import java.util.*;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface PartnerOutboxRepository extends JpaRepository<PartnerOutboxEvent,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE)List<PartnerOutboxEvent>findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
