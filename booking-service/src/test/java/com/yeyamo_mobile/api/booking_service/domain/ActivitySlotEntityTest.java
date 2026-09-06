package com.yeyamo_mobile.api.booking_service.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.ActivitySlotEntity;

class ActivitySlotEntityTest {

    private ActivitySlotEntity slot() {
        return ActivitySlotEntity.create("activity-1", "partner-1", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 3, new BigDecimal("12.50"), "eur");
    }

    @Test
    void reservesAndReleasesCapacity() {
        var s = slot();
        s.reserve(2);
        assertEquals(1, s.available());
        s.release(1);
        assertEquals(2, s.available());
    }

    @Test
    void rejectsOverbooking() {
        var s = slot();
        assertThrows(BookingException.class, () -> s.reserve(4));
    }

    @Test
    void closedSlotIsUnavailable() {
        var s = slot();
        s.close();
        assertThrows(BookingException.class, () -> s.reserve(1));
    }

    @Test
    void validatesTimeRange() {
        assertThrows(BookingException.class, () -> ActivitySlotEntity.create("a", "partner-1", Instant.now().plusSeconds(2), Instant.now(), 1, BigDecimal.ONE, "EUR"));
    }

    @Test
    void rejectsPriceWithMoreThanTwoDecimals() {
        assertThrows(BookingException.class, () -> ActivitySlotEntity.create("a", "partner-1", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 1, new BigDecimal("10.999"), "EUR"));
    }

    @Test
    void createsSlotWithPlaceId() {
        UUID placeId = UUID.randomUUID();
        var s = ActivitySlotEntity.create("activity-1", "partner-1", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, new BigDecimal("25.00"), "EUR", "CI", placeId);
        assertNotNull(s.getId());
        assertEquals(placeId, s.getPlaceId());
        assertEquals("activity-1", s.getActivityId());
        assertEquals("partner-1", s.getOwnerUserId());
        assertEquals("CI", s.getCountryCode());
    }
}
