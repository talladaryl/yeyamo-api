package com.yeyamo_mobile.api.event_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.event_service.models.EventSocialDistributionReceipt;

public interface EventSocialDistributionReceiptRepository
        extends JpaRepository<EventSocialDistributionReceipt, UUID> {
}
