package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedCampaignEventRepository
        extends JpaRepository<ProcessedCampaignEvent, UUID> {
}
