package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_orders")
public class TicketOrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String reference;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketOrderStatus status = TicketOrderStatus.CREATED;
    
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "PENDING";
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "service_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;
    
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_operator", length = 20)
    private String paymentOperator;

    @Column(name = "payment_phone_number", length = 16)
    private String paymentPhoneNumber;

    @Column(name = "payment_country_code", length = 2)
    private String paymentCountryCode;
    
    @Column(name = "promotion_id")
    private String promotionId;
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "paid_at")
    private Instant paidAt;
    
    @Column(name = "issued_at")
    private Instant issuedAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public TicketOrderStatus getStatus() { return status; }
    public void setStatus(TicketOrderStatus status) { this.status = status; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { 
        this.paymentStatus = paymentStatus; 
    }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { 
        this.discountAmount = discountAmount; 
    }
    
    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentOperator() { return paymentOperator; }
    public void setPaymentOperator(String paymentOperator) { this.paymentOperator = paymentOperator; }

    public String getPaymentPhoneNumber() { return paymentPhoneNumber; }
    public void setPaymentPhoneNumber(String paymentPhoneNumber) { this.paymentPhoneNumber = paymentPhoneNumber; }

    public String getPaymentCountryCode() { return paymentCountryCode; }
    public void setPaymentCountryCode(String paymentCountryCode) { this.paymentCountryCode = paymentCountryCode; }
    
    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }
    
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { 
        this.paymentReference = paymentReference; 
    }
    
    public String getPaymentProvider() { return paymentProvider; }
    public void setPaymentProvider(String paymentProvider) { 
        this.paymentProvider = paymentProvider; 
    }
    
    public Instant getCreatedAt() { return createdAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
}
