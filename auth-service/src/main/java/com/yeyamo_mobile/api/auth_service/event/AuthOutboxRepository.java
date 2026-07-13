package com.yeyamo_mobile.api.auth_service.event;
import java.util.*;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface AuthOutboxRepository extends JpaRepository<AuthOutboxEvent,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE)List<AuthOutboxEvent>findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
