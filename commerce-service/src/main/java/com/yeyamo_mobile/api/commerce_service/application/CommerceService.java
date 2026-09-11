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
    private final CanonicalPriceResolver canonicalPrices;
    private final PricingEngine pricing = new PricingEngine();

    public CommerceService(OrderRepository orders, OrderLineRepository lines,
            PromotionRepository promotions, PromotionUsageRepository usages,
            CommissionRepository commissions, LedgerRepository ledger,
            CommerceRefundRepository refunds, InvoiceRepository invoices,
            OutboxService outbox, CanonicalPriceResolver canonicalPrices) {
        this.orders = orders;
        this.lines = lines;
        this.promotions = promotions;
        this.usages = usages;
        this.commissions = commissions;
        this.ledger = ledger;
        this.refunds = refunds;
        this.invoices = invoices;
        this.outbox = outbox;
        this.canonicalPrices = canonicalPrices;
    }

    public record Line(String productId, String description, int quantity, BigDecimal unitPrice) {}
    /**
     * Customer-entered data needed by an external cash-in provider. Monetary
     * values are intentionally absent: pricing is resolved from the locked
     * server-side offer.
     */
    public record CashInDetails(String operator, String country, String phoneNumber) {}
    public record Create(String userId, String partnerId, String sourceEntityId,
        ProductType productType, String currency, List<Line> lines,
        String offerId, String promotionCode, BigDecimal tax, BigDecimal serviceFee,
        CashInDetails cashInDetails) {}

    @Transactional
    public CommerceOrder create(Create command, String idempotencyKey) {
        Optional<CommerceOrder> replay = orders.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) return replay.get();
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("At least one line is required");
        }

        int quantity = command.lines().stream().mapToInt(Line::quantity).sum();
        CanonicalPriceResolver.Price canonical = canonicalPrices.resolve(new CanonicalPriceResolver.Request(
            command.productType(), command.sourceEntityId(), quantity, command.userId(), command.offerId()));
        BigDecimal subtotal = canonical.unitPrice().multiply(BigDecimal.valueOf(canonical.quantity()));

        Promotion promotion = resolvePromotion(command, canonical.partnerId(), subtotal);
        CommissionRule commission = resolveCommission(command.productType(), canonical.partnerId());
        PricingEngine.Result amounts = pricing.calculate(new PricingEngine.Input(
            subtotal, BigDecimal.ZERO, BigDecimal.ZERO,
            promotion == null ? null : promotion.discountType,
            promotion == null ? null : promotion.discountValue,
            promotion == null ? null : promotion.maximumDiscount,
            commission == null ? null : commission.percentage,
            commission == null ? null : commission.fixedAmount,
            commission == null ? null : commission.maximumAmount,
            canonical.currency()
        ));

        CashInDetails cashInDetails = amounts.totalAmount().signum() == 0
            ? null : requiredCashInDetails(command.cashInDetails());
        CommerceOrder order = new CommerceOrder();
        order.id = UUID.randomUUID();
        order.reference = "COM-" + order.id.toString().substring(0, 8).toUpperCase();
        order.idempotencyKey = idempotencyKey;
        order.userId = command.userId();
        order.partnerId = canonical.partnerId();
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
        order.currency = canonical.currency().toUpperCase(Locale.ROOT);
        order.promotionId = promotion == null ? null : promotion.id;
        order.pricingRuleVersion = 1;
        order.commissionRuleVersion = commission == null ? null : commission.ruleVersion;
        if (cashInDetails != null) {
            order.paymentOperator = cashInDetails.operator();
            order.paymentCountryCode = cashInDetails.country();
            order.paymentPhoneNumber = cashInDetails.phoneNumber();
        }
        orders.save(order);

        saveLine(canonical, order);
        if (promotion != null) savePromotionUsage(promotion, order);
        if (promotion != null) {
            Map<String, Object> promotionPayload = new LinkedHashMap<>();
            promotionPayload.put("orderId", order.id);
            promotionPayload.put("partnerId", order.partnerId);
            promotionPayload.put("sourceEntityId", order.sourceEntityId);
            promotionPayload.put("promotionId", promotion.id);
            promotionPayload.put("amount", order.discountAmount);
            promotionPayload.put("currency", order.currency);
            outbox.append("commerce.events", "promotion.applied",
                order.id.toString(), idempotencyKey, promotionPayload);
        }
        if (order.totalAmount.signum() == 0) completeFree(order);
        else requestPayment(order, idempotencyKey);
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
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("orderId", order.id);
        eventPayload.put("partnerId", order.partnerId);
        eventPayload.put("sourceEntityId", order.sourceEntityId);
        eventPayload.put("amount", order.totalAmount);
        eventPayload.put("currency", order.currency);
        eventPayload.put("paymentId", paymentId == null ? "" : paymentId);
        outbox.append("commerce.events", "payment.confirmed",
            order.id.toString(), order.id.toString(), eventPayload);
        Map<String, Object> commissionPayload = new LinkedHashMap<>(eventPayload);
        commissionPayload.put("amount", order.commissionAmount);
        commissionPayload.put("ruleVersion",
            order.commissionRuleVersion == null ? 0 : order.commissionRuleVersion);
        outbox.append("commerce.events", "commission.calculated",
            order.id.toString(), order.id.toString(), commissionPayload);
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
                "sourceType", "COMMERCE_ORDER",
                "sourceId", orderId,
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
            entry.balanceAfter = ledger.balance(partner, currency).add(amount);
            entry.reference = type.name() + "-" + entry.id;
            entry.idempotencyKey = key;
            entry.occurredAt = Instant.now();
            entry.createdBy = actor;
            entry.reason = reason;
            LedgerEntry saved = ledger.save(entry);
            if (type == LedgerType.PAYOUT) {
                outbox.append("commerce.events", "settlement.completed",
                    partner, key, Map.of(
                        "partnerId", partner,
                        "amount", amount,
                        "currency", currency,
                        "reference", saved.reference
                    ));
            }
            return saved;
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

    private Promotion resolvePromotion(Create command, String canonicalPartnerId, BigDecimal subtotal) {
        if (command.promotionCode() == null || command.promotionCode().isBlank()) return null;
        Promotion promotion = promotions.lockedByCode(command.promotionCode().trim())
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
        validatePromotion(promotion, command, canonicalPartnerId, subtotal);
        promotion.usageCount++;
        return promotion;
    }

    private CommissionRule resolveCommission(ProductType productType, String canonicalPartnerId) {
        List<CommissionRule> active = commissions.active(
            canonicalPartnerId, productType, Instant.now());
        return active.stream().filter(rule -> canonicalPartnerId.equals(rule.partnerId)).findFirst()
            .orElseGet(() -> active.stream().findFirst().orElse(null));
    }

    private void validatePromotion(Promotion promotion, Create command,
            String canonicalPartnerId, BigDecimal subtotal) {
        Instant now = Instant.now();
        if (promotion.status != PromotionStatus.ACTIVE
                || now.isBefore(promotion.startsAt) || !now.isBefore(promotion.endsAt)) {
            throw new IllegalArgumentException("Promotion inactive");
        }
        if (promotion.partnerId != null
                && !promotion.partnerId.equals(canonicalPartnerId)) {
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

    private void saveLine(CanonicalPriceResolver.Price source, CommerceOrder order) {
            OrderLine line = new OrderLine();
            line.id = UUID.randomUUID();
            line.orderId = order.id;
            line.productId = source.productId(); line.description = source.description(); line.quantity = source.quantity(); line.unitPrice = source.unitPrice();
            line.lineTotal = source.unitPrice()
                .multiply(BigDecimal.valueOf(source.quantity()));
            line.priceSnapshot = "{\"unitPrice\":\""
                + source.unitPrice().toPlainString() + "\",\"currency\":\""
                + order.currency + "\",\"source\":\"" + source.canonicalSource() + "\"}";
            lines.save(line);
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
                "sourceType", "COMMERCE_ORDER",
                "sourceId", order.id,
                "userId", order.userId,
                "amount", order.totalAmount,
                "currency", order.currency,
                "operator", order.paymentOperator,
                "country", order.paymentCountryCode,
                "phoneNumber", order.paymentPhoneNumber,
                "idempotencyKey", "commerce:" + order.id + ":payment"
            ));
    }

    private CashInDetails requiredCashInDetails(CashInDetails details) {
        if (details == null) throw new IllegalArgumentException("PAYMENT_DETAILS_REQUIRED");
        String operator = details.operator() == null ? "" : details.operator().trim().toLowerCase(Locale.ROOT);
        if (!Set.of("mtn", "orange", "moov", "airtel", "mpesa", "wave", "free", "tmoney", "afrimoney").contains(operator)) {
            throw new IllegalArgumentException("PAYMENT_OPERATOR_REQUIRED");
        }
        String country = details.country() == null ? "" : details.country().trim().toUpperCase(Locale.ROOT);
        if (!country.matches("[A-Z]{2}")) throw new IllegalArgumentException("PAYMENT_COUNTRY_REQUIRED");
        String phoneNumber = details.phoneNumber() == null ? "" : details.phoneNumber().trim();
        if (!phoneNumber.matches("\\+[1-9]\\d{1,14}")) throw new IllegalArgumentException("PAYMENT_PHONE_REQUIRED");
        return new CashInDetails(operator, country, phoneNumber);
    }

    private void completeFree(CommerceOrder order) {
        order.status = OrderStatus.COMPLETED;
        orders.save(order);
        issueInvoice(order);
        outbox.append("commerce.events", "commerce.free.completed", order.id.toString(), order.id.toString(), Map.of(
            "orderId", order.id, "amount", order.totalAmount, "currency", order.currency));
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
        entry.balanceAfter = ledger.balance(order.partnerId, order.currency).add(amount);
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
