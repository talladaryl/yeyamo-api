package com.yeyamo_mobile.api.admin_service.governance;
import java.util.*;import org.springframework.data.jpa.repository.*;
interface PlatformSettingRepository extends JpaRepository<PlatformSettingEntity,String>{}
interface PlatformSettingHistoryRepository extends JpaRepository<PlatformSettingHistoryEntity,UUID>{List<PlatformSettingHistoryEntity>findByKeyOrderByVersionDesc(String key);Optional<PlatformSettingHistoryEntity>findByKeyAndVersion(String key,long version);}
interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity,String>{}
interface FeatureFlagHistoryRepository extends JpaRepository<FeatureFlagHistoryEntity,UUID>{List<FeatureFlagHistoryEntity>findByKeyOrderByVersionDesc(String key);Optional<FeatureFlagHistoryEntity>findByKeyAndVersion(String key,long version);}
