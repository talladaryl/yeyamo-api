package com.yeyamo_mobile.api.admin_service.event;
import java.util.*;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface AdminOutboxRepository extends JpaRepository<AdminOutboxEvent,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE)List<AdminOutboxEvent>findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
