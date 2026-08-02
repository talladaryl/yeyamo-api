package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAdDeliveryRecordRepository extends JpaRepository<AdDeliveryRecordEntity, String> {
    boolean existsByDeliveryId(String deliveryId);
}
