package com.yeyamo_mobile.api.notification_service.admin;
import java.time.Instant;import java.util.*;import org.springframework.data.jpa.repository.*;
public interface AdminNotificationRepository extends JpaRepository<AdminNotificationEntity,UUID>,JpaSpecificationExecutor<AdminNotificationEntity>{boolean existsBySourceEventId(UUID id);long deleteByExpiresAtBefore(Instant cutoff);}
