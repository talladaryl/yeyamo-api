package com.yeyamo_mobile.api.notification_service.application.port;
import java.util.Optional;import com.yeyamo_mobile.api.notification_service.domain.NotificationPreference;
public interface PreferenceRepositoryPort{Optional<NotificationPreference>find(String userId);NotificationPreference save(NotificationPreference preference);}
