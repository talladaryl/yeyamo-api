package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.AdDeliveryRecord;
import java.util.Optional;

public interface AdDeliveryRecordRepository {
    void save(AdDeliveryRecord record);
    Optional<AdDeliveryRecord> findByDeliveryId(String deliveryId);
    boolean existsByDeliveryId(String deliveryId);
}
