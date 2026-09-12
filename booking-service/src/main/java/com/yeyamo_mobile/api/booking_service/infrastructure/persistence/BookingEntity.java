package com.yeyamo_mobile.api.booking_service.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.booking_service.domain.ActivityType;
import com.yeyamo_mobile.api.booking_service.domain.BookingException;
import com.yeyamo_mobile.api.booking_service.domain.BookingStatus;
import com.yeyamo_mobile.api.booking_service.domain.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "bookings")
public class BookingEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 24) private String reference;
    @Column(name = "user_id", nullable = false, length = 120) private String userId;
    @Column(name = "activity_id", nullable = false, length = 120) private String activityId;
    @Enumerated(EnumType.STRING) @Column(name = "activity_type", nullable = false, length = 20) private ActivityType activityType = ActivityType.ACTIVITY;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "slot_id") private ActivitySlotEntity slot;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "country_code", length = 2) private String countryCode;
    @Column(name = "payment_country_code", length = 2) private String paymentCountryCode;
    @Column(name = "payment_operator", length = 20) private String paymentOperator;
    @Column(name = "payment_phone_number", length = 16) private String paymentPhoneNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 30) private PaymentStatus paymentStatus;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    public static BookingEntity pending(String reference, String user, ActivitySlotEntity slot, int quantity) {
        return pending(reference, user, slot, quantity, null, null, null);
    }

    public static BookingEntity pending(String reference, String user, ActivitySlotEntity slot, int quantity,
            String operator, String phoneNumber, String paymentCountryCode) {
        BookingEntity booking = new BookingEntity();
        booking.id = UUID.randomUUID();
        booking.reference = reference;
        booking.userId = user;
        booking.activityId = slot.getActivityId();
        booking.activityType = slot.getActivityType();
        booking.slot = slot;
        booking.quantity = quantity;
        booking.unitPrice = slot.getUnitPrice();
        booking.totalAmount = booking.unitPrice.multiply(BigDecimal.valueOf(quantity));
        booking.currency = slot.getCurrency();
        booking.countryCode = slot.getCountryCode();
        booking.paymentCountryCode = paymentCountryCode;
        booking.paymentOperator = operator;
        booking.paymentPhoneNumber = phoneNumber;
        booking.status = BookingStatus.PENDING;
        booking.paymentStatus = booking.totalAmount.signum() == 0 ? PaymentStatus.NOT_REQUIRED : PaymentStatus.AUTHORIZATION_PENDING;
        booking.createdAt = Instant.now();
        booking.updatedAt = booking.createdAt;
        return booking;
    }

    public void confirm() { require(BookingStatus.PENDING); status = BookingStatus.CONFIRMED; paymentStatus = totalAmount.signum() == 0 ? PaymentStatus.NOT_REQUIRED : PaymentStatus.AUTHORIZED; confirmedAt = Instant.now(); updatedAt = confirmedAt; }
    public void paymentFailed(String reason) { if (status != BookingStatus.PENDING) return; status = BookingStatus.CANCELLED; paymentStatus = PaymentStatus.FAILED; cancellationReason = reason; cancelledAt = Instant.now(); updatedAt = cancelledAt; }
    public void paymentFailedAfterCancellation(String reason) { if (status == BookingStatus.CANCELLED && paymentStatus == PaymentStatus.AUTHORIZATION_PENDING) { paymentStatus = PaymentStatus.FAILED; updatedAt = Instant.now(); } }
    public void authorizationAfterCancellation() { if (status != BookingStatus.CANCELLED || paymentStatus != PaymentStatus.AUTHORIZATION_PENDING) throw invalid(); paymentStatus = PaymentStatus.REFUND_PENDING; updatedAt = Instant.now(); }
    public void authorizationAfterCancellationWithoutRefund() { if (status != BookingStatus.CANCELLED || paymentStatus != PaymentStatus.AUTHORIZATION_PENDING) throw invalid(); paymentStatus = PaymentStatus.REFUND_NOT_REQUESTED; updatedAt = Instant.now(); }
    public void cancel(String reason) { if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) throw invalid(); boolean paid = paymentStatus == PaymentStatus.AUTHORIZED; status = BookingStatus.CANCELLED; paymentStatus = paid ? PaymentStatus.REFUND_PENDING : paymentStatus; cancellationReason = reason; cancelledAt = Instant.now(); updatedAt = cancelledAt; }
    /** Experience cancellation retains the settled payment; a separate support/refund workflow is required. */
    public void cancelWithoutAutomaticRefund(String reason) { if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) throw invalid(); boolean paid = paymentStatus == PaymentStatus.AUTHORIZED; status = BookingStatus.CANCELLED; paymentStatus = paid ? PaymentStatus.REFUND_NOT_REQUESTED : paymentStatus; cancellationReason = reason; cancelledAt = Instant.now(); updatedAt = cancelledAt; }
    public void refunded() { if (status != BookingStatus.CANCELLED || paymentStatus != PaymentStatus.REFUND_PENDING) throw invalid(); paymentStatus = PaymentStatus.REFUNDED; updatedAt = Instant.now(); }
    public void complete() { require(BookingStatus.CONFIRMED); status = BookingStatus.COMPLETED; completedAt = Instant.now(); updatedAt = completedAt; }

    private void require(BookingStatus expected) { if (status != expected) throw invalid(); }
    private BookingException invalid() { return new BookingException("INVALID_BOOKING_TRANSITION", "Invalid booking state transition"); }

    public UUID getId() { return id; } public String getReference() { return reference; } public String getUserId() { return userId; }
    public String getActivityId() { return activityId; } public ActivityType getActivityType() { return activityType; }
    public ActivitySlotEntity getSlot() { return slot; } public int getQuantity() { return quantity; } public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; } public String getCurrency() { return currency; } public String getCountryCode() { return countryCode; }
    public String getPaymentCountryCode() { return paymentCountryCode; } public String getPaymentOperator() { return paymentOperator; }
    public String getPaymentPhoneNumber() { return paymentPhoneNumber; } public BookingStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; } public String getCancellationReason() { return cancellationReason; }
    public Instant getCreatedAt() { return createdAt; } public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; } public Instant getCompletedAt() { return completedAt; }
}
