package com.yeyamo_mobile.api.commerce_service.persistence;
import jakarta.persistence.*;import java.math.*;import java.time.*;import java.util.*;
@Entity @Table(name="commerce_refunds")public class CommerceRefund{@Id public UUID id;@Column(name="order_id")public UUID orderId;public BigDecimal amount;public String status;@Column(name="idempotency_key")public String idempotencyKey;@Column(name="payment_refund_id")public String paymentRefundId;public String reason;@Column(name="created_at")public Instant createdAt;@Column(name="completed_at")public Instant completedAt;}
