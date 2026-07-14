package com.yeyamo_mobile.api.notification_service.application.port;
import java.util.*;import com.yeyamo_mobile.api.notification_service.application.NotificationSlice;import com.yeyamo_mobile.api.notification_service.domain.Notification;
public interface NotificationRepositoryPort{Notification save(Notification n);Optional<Notification>findById(UUID id);boolean exists(UUID sourceEventId,String recipientId);NotificationSlice findByRecipient(String recipient,int page,int size);int markAllRead(String recipient);}
