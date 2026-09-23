package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanItemRepository;

/** Updates only the critical availability state of persisted Event snapshots. */
@Service
public class AdventurePlanAvailabilityService {
    private final AdventurePlanItemRepository items;

    public AdventurePlanAvailabilityService(AdventurePlanItemRepository items) {
        this.items = items;
    }

    @Transactional
    public void markEventUnavailable(String eventId) {
        items.updateAvailabilityForTarget(AdventureTargetType.EVENT, eventId,
                AdventureAvailabilityStatus.CANCELLED);
    }

    @Transactional
    public void markEventCompleted(String eventId) {
        items.updateAvailabilityForTarget(AdventureTargetType.EVENT, eventId,
                AdventureAvailabilityStatus.UNAVAILABLE);
    }
}
