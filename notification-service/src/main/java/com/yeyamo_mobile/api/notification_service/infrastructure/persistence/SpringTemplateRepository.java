package com.yeyamo_mobile.api.notification_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;import com.yeyamo_mobile.api.notification_service.domain.NotificationChannel;
public interface SpringTemplateRepository extends JpaRepository<TemplateEntity,UUID>{Optional<TemplateEntity>findByEventTypeAndChannelAndLocaleAndActiveTrue(String eventType,NotificationChannel channel,String locale);}
