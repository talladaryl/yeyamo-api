package com.yeyamo_mobile.api.notification_service.admin;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminNotificationReadRepository extends JpaRepository<AdminNotificationReadEntity,UUID>{Optional<AdminNotificationReadEntity>findByNotificationIdAndAdminId(UUID notificationId,String adminId);Set<AdminNotificationReadEntity>findByAdminId(String adminId);void deleteByNotificationIdAndAdminId(UUID notificationId,String adminId);}
