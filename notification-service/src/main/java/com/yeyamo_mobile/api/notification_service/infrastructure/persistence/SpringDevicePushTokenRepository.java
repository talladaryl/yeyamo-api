package com.yeyamo_mobile.api.notification_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringDevicePushTokenRepository extends JpaRepository<DevicePushTokenEntity,UUID>{Optional<DevicePushTokenEntity>findByUserIdAndDeviceId(String userId,String deviceId);Optional<DevicePushTokenEntity>findByToken(String token);List<DevicePushTokenEntity>findByUserIdAndEnabledTrue(String userId);}
