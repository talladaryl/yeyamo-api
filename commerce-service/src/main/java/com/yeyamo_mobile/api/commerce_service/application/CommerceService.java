package com.yeyamo_mobile.api.commerce_service.application;

import com.yeyamo_mobile.api.commerce_service.domain.PricingEngine;
import com.yeyamo_mobile.api.commerce_service.messaging.OutboxService;
import com.yeyamo_mobile.api.commerce_service.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.*;

@Service
public class CommerceService {
    private final OrderRepository orders;
    private final OrderLineRepository lines;
    private final PromotionRepository promotions;
    private final PromotionUsageRepository usages;
    private final CommissionRepository commissions;
    private final LedgerRepository ledger;
    private final CommerceRefundRepository refunds;
    private final InvoiceRepository invoices;
    private final OutboxService outbox;
    private final PricingEngine pricing = new PricingEngine();

    public CommerceService(OrderRepository orders, OrderLineRepository lines,
            PromotionRepository promotions, PromotionUsageRepository usages,
            CommissionRepository commissions, LedgerRepository ledger,
            CommerceRefundRepository refunds, InvoiceRepository invoices,
            OutboxService outbox) {
        this.orders = orders;
        this.lines = lines;
        this.promotions = promotions;
        this.usages = usages;
        this.commissions = commissions;
        this.ledger = ledger;
        this.refunds = refunds;
        this.invoices = invoices;
        this.outbox = outbox;
    }

    public record Line(String productId, String description, int quantity, BigDecimal unitPrice) {}
    public record Create(String userId, String partnerId, String sourceEntityId,
        ProductType productType, String currency, List<Line> lines,
        String promotionCode, BigDecimal tax, BigDecimal serviceFee) {}

    @Transactional
    public CommerceOrder create(Create command, String idempotencyKey) {
        Optional<CommerceOrder> replay = orders.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) return replay.get();
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("At least one line is required");
        }

        BigDecimal subtotal = command.lines().stream().map(line -> {
            if (line.quantity() <= 0 || line.unitPrice() == null || line.unitPrice().signum() < 0) {
                throw new IllegalArgumentException("Invalid line");
            }
            return line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);

        Promotion promotion = resolvePromotion(command, subtotal);
        CommissionRule commission = resolveCommission(command);
        PricingEngine.Result amounts = pricing.calculate(new PricingEngine.Input(
            subtotal, command.tax(), command.serviceFee(),
            promotion == null ? null : promotion.discountType,
            promotion == null ? null : promotion.discountValue,
            promotion == null ? null : promotion.maximumDiscount,
            commission == null ? null : commission.percentage,
            commission == null ? null : commission.fixedAmount,
            commission == null ? null : commission.maximumAmount,
            command.currency()
        ));

        CommerceOrder order = new CommerceOrder();
        order.id = UUID.randomUUID();
        order.reference = "COM-" + order.id.toString().substring(0, 8).toUpperCase();
        order.idempotencyKey = idempotencyKey;
        order.userId = command.userId();
        order.partnerId = command.partnerId();
        order.sourceEntityId = command.sourceEntityId();
        order.productType = command.productType();
        order.status = OrderStatus.AWAITING_PAYMENT;
        order.subtotal = amounts.subtotal();
        order.discountAmount = amounts.discountAmount();
        order.taxAmount = amounts.taxAmount();
        order.serviceFee = amounts.serviceFee();
        order.commissionAmount = amounts.commissionAmount();
        order.totalAmount = amounts.totalAmount();
        order.partnerNetAmount = amounts.partnerNetAmount();
        order.currency = command.currency().toUpperCase(Locale.ROOT);
        order.promotionId = promotion == null ? null : promotion.id;
        order.pricingRuleVersion = 1;
        order.commissionRuleVersion = commission == null ? null : commission.ruleVersion;
        orders.save(order);

        saveLines(command, order);
        if (promotion != null) savePromotionUsage(promotion, order);
        requestPayment(order, idempotencyKey);
        return order;
    }

    @Transactional
    public CommerceOrder paid(UUID orderId, String paymentId) {
        CommerceOrder order = orders.locked(orderId).orElseThrow();
        if (order.status == OrderStatus.PAID || order.status == OrderStatus.COMPLETED) return order;
        if (order.status != OrderStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Order cannot transition to paid");
        }
        order.status = OrderStatus.PAID;
        order.paymentId = paymentId;
        order.paidAt = Instant.now();
        orders.save(order);
        append(order, LedgerType.SALE_CREDIT,
            order.partnerNetAmount.add(order.commissionAmount), "sale:" + orderId);
        append(order, LedgerType.PLATFORM_COMMISSION,
            order.commissionAmount.negate(), "commission:" + orderId);
        issueInvoice(order);
        return order;
    }

    @Transactional
    public void paymentFailed(UUID orderId) {
        CommerceOrder order = orders.locked(orderId).orElseThrow();
        if (order.status == OrderStatus.AWAITING_PAYMENT) {
            order.status = OrderStatus.PAYMENT_FAILED;
            orders.save(order);
        }
    }

    @Transactional
    public CommerceRefund refund(UUID orderId, String actor, BigDecimal amount,
            String idempotencyKey, String reason) {
        Optional<CommerceRefund> replay = refunds.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            CommerceOrder existing = orders.findById(replay.get().orderId).orElseThrow();
            requireOwner(existing, actor);
            return replay.get();
        }

        CommerceOrder order = orders.locked(orderId).orElseThrow();
        requireOwner(order, actor);
        if (order.status != OrderStatus.PAID
                && order.status != OrderStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Order is not refundable");
        }
        BigDecimal previous = Optional.ofNullable(refunds.reserved(orderId))
            .orElse(BigDecimal.ZERO);
        if (amount == null || amount.signum() <= 0
                || previous.add(amount).compareTo(order.totalAmount) > 0) {
            throw new IllegalArgumentException("Invalid refund amount");
        }

        CommerceRefund refund = new CommerceRefund();
        refund.id = UUID.randomUUID();
        refund.orderId = orderId;
        refund.amount = amount;
        refund.status = "REQUESTED";
        refund.idempotencyKey = idempotencyKey;
        refund.reason = reason;
        refund.createdAt = Instant.now();
        refunds.save(refund);
        outbox.append("payment.commands", "payment.refund.requested",
            orderId.toString(), refund.id.toString(), Map.of(
                "sagaId", orderId,
                "bookingId", orderId,
                "paymentId", order.paymentId,
                "amount", amount,
                "currency", order.currency,
                "idempotencyKey", idempotencyKey
            ));
        return refund;
    }

    @Transactional
    public void refundCompleted(UUID orderId, UUID refundId, String paymentRefundId) {
        CommerceOrder order = orders.locked(orderId).orElseThrow();
        CommerceRefund refund = refunds.findById(refundId)
            .filter(row -> row.orderId.equals(orderId))
            .orElseThrow();
        if ("COMPLETED".equals(refund.status)) return;
        if (!"REQUESTED".equals(refund.status)) {
            throw new IllegalStateException("Refund is not pending");
        }
        refund.status = "COMPLETED";
        refund.paymentRefundId = paymentRefundId;
        refund.completedAt = Instant.now();
        refunds.save(refund);
        BigDecimal total = Optional.ofNullable(refunds.completed(orderId))
            .orElse(BigDecimal.ZERO);
        order.status = total.compareTo(order.totalAmount) >= 0
            ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED;
        orders.save(order);
        append(order, LedgerType.REFUND_DEBIT, refund.amount.negate(),
            "refund:" + refund.id);
    }

    @Transactional
    public LedgerEntry adjustment(String partner, UUID order, BigDecimal amount,
            String currency, String key, String actor, String reason) {
        return movement(partner, order, LedgerType.ADJUSTMENT, amount,
            currency, key, actor, reason);
    }

    @Transactional
    public LedgerEntry movement(String partner, UUID order, LedgerType type,
            BigDecimal amount, String currency, String key, String actor, String reason) {
        if (type == LedgerType.SALE_CREDIT || type == LedgerType.PLATFORM_COMMISSION
                || type == LedgerType.REFUND_DEBIT) {
            throw new IllegalArgumentException("System movement type");
        }
        return ledger.findByIdempotencyKey(key).orElseGet(() -> {
            LedgerEntry entry = new LedgerEntry();
            entry.id = UUID.randomUUID();
            entry.partnerId = partner;
            entry.orderId = order;
            entry.transactionType = type;
            entry.amount = amount;
            entry.currency = currency;
            entry.reference = type.name() + "-" + entry.id;
            entry.idempotencyKey = key;
            entry.occurredAt = Instant.now();
            entry.createdBy = actor;
            entry.reason = reason;
            return ledger.save(entry);
        });
    }

    @Transactional(readOnly = true)
    public List<CommerceOrder> mine(String user) {
        return orders.findByUserIdOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> ledger(String partner) {
        return ledger.findByPartnerIdOrderByOccurredAtAsc(partner);
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(String partner, String currency) {
        return ledger.balance(partner, currency);
    }

    private Promotion resolvePromotion(Create command, BigDecimal subtotal) {
        if (command.promotionCode() == null || command.promotionCode().isBlank()) return null;
        Promotion promotion = promotions.lockedByCode(command.promotionCode().trim())
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
        validatePromotion(promotion, command, subtotal);
        promotion.usageCount++;
        return promotion;
    }

    private CommissionRule resolveCommission(Create command) {
        List<CommissionRule> active = commissions.active(
            command.partnerId(), command.productType(), Instant.now());
        return active.stream().filter(rule -> rule.partnerId != null).findFirst()
            .orElseGet(() -> active.stream().findFirst().orElse(null));
    }

    private void validatePromotion(Promotion promotion, Create command, BigDecimal subtotal) {
        Instant now = Instant.now();
        if (promotion.status != PromotionStatus.ACTIVE
                || now.isBefore(promotion.startsAt) || !now.isBefore(promotion.endsAt)) {
            throw new IllegalArgumentException("Promotion inactive");
        }
        if (promotion.partnerId != null
                && !promotion.partnerId.equals(command.partnerId())) {
            throw new IllegalArgumentException("Promotion not applicable");
        }
        if (subtotal.compareTo(promotion.minimumOrderAmount) < 0) {
            throw new IllegalArgumentException("Minimum amount not reached");
        }
        if (promotion.usageLimit != null && promotion.usageCount >= promotion.usageLimit) {
            throw new IllegalArgumentException("Promotion exhausted");
        }
        if (promotion.usageLimitPerUser != null
                && usages.countByPromotionIdAndUserId(
                    promotion.id, command.userId()) >= promotion.usageLimitPerUser) {
            throw new IllegalArgumentException("Promotion user limit reached");
        }
        if (!containsOrUnrestricted(
                promotion.applicableProductTypes, command.productType().name())) {
            throw new IllegalArgumentException("Promotion product not applicable");
        }
        if (!containsOrUnrestricted(
                promotion.applicableEntityIds, command.sourceEntityId())) {
            throw new IllegalArgumentException("Promotion entity not applicable");
        }
    }

    private boolean containsOrUnrestricted(String csv, String value) {
        return csv == null || csv.isBlank() || Set.of(csv.split(",")).contains(value);
    }

    private void saveLines(Create command, CommerceOrder order) {
        for (Line source : command.lines()) {
            OrderLine line = new OrderLine();
            line.id = UUID.randomUUID();
            line.orderId = order.id;
            line.productId = source.productId();
            line.description = source.description();
            line.quantity = source.quantity();
            line.unitPrice = source.unitPrice();
            line.lineTotal = source.unitPrice()
                .multiply(BigDecimal.valueOf(source.quantity()));
            line.priceSnapshot = "{\"unitPrice\":\""
                + source.unitPrice().toPlainString() + "\",\"currency\":\""
                + order.currency + "\"}";
            lines.save(line);
        }
    }

    private void savePromotionUsage(Promotion promotion, CommerceOrder order) {
        PromotionUsage usage = new PromotionUsage();
        usage.id = UUID.randomUUID();
        usage.promotionId = promotion.id;
        usage.orderId = order.id;
        usage.userId = order.userId;
        usage.usedAt = Instant.now();
        usages.save(usage);
    }

    private void requestPayment(CommerceOrder order, String correlationId) {
        outbox.append("payment.commands", "payment.authorization.requested",
            order.id.toString(), correlationId, Map.of(
                "sagaId", order.id,
                "bookingId", order.id,
                "userId", order.userId,
                "amount", order.totalAmount,
                "currency", order.currency,
                "idempotencyKey", "commerce:" + order.id + ":payment"
            ));
    }

    private void append(CommerceOrder order, LedgerType type,
            BigDecimal amount, String key) {
        if (ledger.findByIdempotencyKey(key).isPresent()) return;
        LedgerEntry entry = new LedgerEntry();
        entry.id = UUID.randomUUID();
        entry.partnerId = order.partnerId;
        entry.orderId = order.id;
        entry.transactionType = type;
        entry.amount = amount;
        entry.currency = order.currency;
        entry.reference = order.reference;
        entry.idempotencyKey = key;
        entry.occurredAt = Instant.now();
        entry.createdBy = "commerce-service";
        ledger.save(entry);
    }

    private void issueInvoice(CommerceOrder order) {
        if (invoices.findByOrderId(order.id).isPresent()) return;
        Invoice invoice = new Invoice();
        invoice.id = UUID.randomUUID();
        invoice.orderId = order.id;
        invoice.invoiceNumber = "INV-" + order.reference;
        invoice.status = "ISSUED";
        invoice.subtotal = order.subtotal;
        invoice.taxAmount = order.taxAmount;
        invoice.totalAmount = order.totalAmount;
        invoice.currency = order.currency;
        invoice.billingSnapshot = "{\"userId\":\"" + order.userId
            + "\",\"partnerId\":\"" + order.partnerId + "\"}";
        invoice.issuedAt = Instant.now();
        invoices.save(invoice);
    }

    private void requireOwner(CommerceOrder order, String actor) {
        if (!order.userId.equals(actor)) {
            throw new SecurityException("Order does not belong to user");
        }
    }
}
