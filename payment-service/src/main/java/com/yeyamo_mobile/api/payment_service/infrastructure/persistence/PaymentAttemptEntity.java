package com.yeyamo_mobile.api.payment_service.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.payment_service.domain.PaymentAttemptStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "payment_attempts",
    indexes = {
        @Index(name = "idx_payment_attempts_reference", columnList = "reference", unique = true),
        @Index(name = "idx_payment_attempts_source", columnList = "source_service, source_id"),
        @Index(name = "idx_payment_attempts_idempotency", columnList = "idempotency_key")
    }
)
public class PaymentAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "reference", nullable = false, unique = true, length = 160)
    private String reference;

    @Column(name = "transaction_id", length = 160)
    private String transactionId;

    @Column(name = "source_service", nullable = false, length = 60)
    private String sourceService;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private PaymentAttemptStatus status;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentAttemptEntity() {}

    public static PaymentAttemptEntity initiated(
            String reference,
            String transactionId,
            String sourceService,
            UUID sourceId,
            BigDecimal amount,
            String currency,
            String idempotencyKey) {
        var entity = new PaymentAttemptEntity();
        entity.id = UUID.randomUUID();
        entity.reference = reference;
        entity.transactionId = transactionId;
        entity.sourceService = sourceService;
        entity.sourceId = sourceId;
        entity.status = PaymentAttemptStatus.INITIATED;
        entity.amount = amount != null ? amount.setScale(2) : BigDecimal.ZERO;
        entity.currency = currency != null ? currency.toUpperCase() : "XOF";
        entity.idempotencyKey = idempotencyKey;
        entity.createdAt = Instant.now();
        entity.updatedAt = entity.createdAt;
        return entity;
    }

    public void markSuccess() {
        this.status = PaymentAttemptStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentAttemptStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markHold(String reason) {
        this.status = PaymentAttemptStatus.HOLD;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getTransactionId() { return transactionId; }
    public String getSourceService() { return sourceService; }
    public UUID getSourceId() { return sourceId; }
    public PaymentAttemptStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
