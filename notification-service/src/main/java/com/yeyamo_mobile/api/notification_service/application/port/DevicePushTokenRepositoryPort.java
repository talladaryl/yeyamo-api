package com.yeyamo_mobile.api.notification_service.application.port;
import java.util.*;import com.yeyamo_mobile.api.notification_service.domain.DevicePushToken;
public interface DevicePushTokenRepositoryPort{DevicePushToken save(DevicePushToken token);Optional<DevicePushToken>findById(UUID id);Optional<DevicePushToken>findByUserIdAndDeviceId(String userId,String deviceId);Optional<DevicePushToken>findByToken(String token);List<DevicePushToken>findEnabledByUserId(String userId);}
