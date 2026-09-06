package com.yeyamo_mobile.api.booking_service.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.booking_service.domain.*;
import jakarta.validation.constraints.*;

public final class BookingDtos {

    private BookingDtos() {}

    public record CreateSlot(
            @NotBlank String activityId,
            UUID placeId,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt,
            @Positive int capacity,
            @NotNull @PositiveOrZero BigDecimal unitPrice,
            @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @Pattern(regexp = "[A-Z]{2}") String countryCode,
            boolean isPaid,
            BigDecimal amount
    ) {
        public CreateSlot(String activityId, UUID placeId, Instant startsAt, Instant endsAt, int capacity, BigDecimal unitPrice, String currency, String countryCode) {
            this(activityId, placeId, startsAt, endsAt, capacity, unitPrice, currency, countryCode, unitPrice != null && unitPrice.signum() > 0, unitPrice);
        }

        public CreateSlot(String activityId, Instant startsAt, Instant endsAt, int capacity, BigDecimal unitPrice, String currency) {
            this(activityId, null, startsAt, endsAt, capacity, unitPrice, currency, null, unitPrice != null && unitPrice.signum() > 0, unitPrice);
        }

        public CreateSlot(String activityId, Instant startsAt, Instant endsAt, int capacity, BigDecimal unitPrice, String currency, String countryCode) {
            this(activityId, null, startsAt, endsAt, capacity, unitPrice, currency, countryCode, unitPrice != null && unitPrice.signum() > 0, unitPrice);
        }
    }

    public record CreateBooking(
            @NotNull UUID slotId,
            @Positive int quantity
    ) {}

    public record CancelBooking(
            @NotBlank @Size(max = 500) String reason
    ) {}

    public record SlotView(
            UUID id,
            String activityId,
            UUID placeId,
            Instant startsAt,
            Instant endsAt,
            int capacity,
            int reserved,
            int available,
            BigDecimal unitPrice,
            String currency,
            String countryCode,
            SlotStatus status,
            boolean isPaid,
            BigDecimal amount
    ) {
        public SlotView(UUID id, String activityId, Instant startsAt, Instant endsAt, int capacity, int reserved, int available, BigDecimal unitPrice, String currency, String countryCode, SlotStatus status) {
            this(id, activityId, null, startsAt, endsAt, capacity, reserved, available, unitPrice, currency, countryCode, status, false, BigDecimal.ZERO);
        }
    }

    public record BookingView(
            UUID id,
            String reference,
            String userId,
            String activityId,
            UUID slotId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String currency,
            String countryCode,
            BookingStatus status,
            PaymentStatus paymentStatus,
            String cancellationReason,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant completedAt
    ) {}

    public record HistoryView(
            UUID id,
            String action,
            String details,
            Instant occurredAt
    ) {}
}
