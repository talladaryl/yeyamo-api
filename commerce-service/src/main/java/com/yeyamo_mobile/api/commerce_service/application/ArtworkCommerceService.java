package com.yeyamo_mobile.api.commerce_service.application;

import com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.ProductType;
import com.yeyamo_mobile.api.commerce_service.messaging.OutboxService;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkCommerceRepositories;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkOffer;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkOrder;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkOrderHistory;
import com.yeyamo_mobile.api.commerce_service.persistence.CommerceOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtworkCommerceService {
    private final ArtworkCommerceRepositories.Offers offers;
    private final ArtworkCommerceRepositories.Orders orders;
    private final ArtworkCommerceRepositories.History history;
    private final CommerceService commerce;
    private final OutboxService outbox;

    public ArtworkCommerceService(ArtworkCommerceRepositories.Offers offers, ArtworkCommerceRepositories.Orders orders,
            ArtworkCommerceRepositories.History history, CommerceService commerce, OutboxService outbox) {
        this.offers = offers;
        this.orders = orders;
        this.history = history;
        this.commerce = commerce;
        this.outbox = outbox;
    }

    public record OfferCommand(UUID artworkId, String artisanPartnerId, ArtworkOffer.SaleType saleType, BigDecimal amount,
            String currencyCode, int availableQuantity, String countryCode, boolean internationalShipping,
            boolean customOrderAllowed, ArtworkOffer.Status status) {
    }

    public record OrderCommand(UUID offerId, int quantity, ArtworkOrder.DeliveryType deliveryType) {
    }

    @Transactional
    public ArtworkOffer createOffer(OfferCommand command, String actor, boolean admin) {
        requirePartner(command.artisanPartnerId(), actor, admin);
        validateOffer(command);
        if (offers.findByArtworkId(command.artworkId()).isPresent()) {
            throw new IllegalArgumentException("Artwork offer already exists");
        }
        ArtworkOffer offer = new ArtworkOffer();
        offer.id = UUID.randomUUID();
        apply(offer, command);
        return offers.save(offer);
    }

    @Transactional
    public ArtworkOffer updateOffer(UUID id, OfferCommand command, String actor, boolean admin) {
        ArtworkOffer offer = offers.locked(id).orElseThrow();
        requirePartner(offer.artisanPartnerId, actor, admin);
        if (!offer.artworkId.equals(command.artworkId()) || !offer.artisanPartnerId.equals(command.artisanPartnerId())) {
            throw new IllegalArgumentException("Offer ownership cannot change");
        }
        validateOffer(command);
        apply(offer, command);
        return offers.save(offer);
    }

    @Transactional
    public ArtworkOffer status(UUID id, ArtworkOffer.Status status, String actor, boolean admin) {
        ArtworkOffer offer = offers.locked(id).orElseThrow();
        requirePartner(offer.artisanPartnerId, actor, admin);
        if (offer.saleType == ArtworkOffer.SaleType.AUCTION_FUTURE && status == ArtworkOffer.Status.ACTIVE) {
            throw new IllegalStateException("Auction is not available in V1");
        }
        offer.status = status;
        return offers.save(offer);
    }

    @Transactional
    public ArtworkOrder createOrder(OrderCommand command, String user, String idempotencyKey) {
        return createOrder(command, user, idempotencyKey, idempotencyKey);
    }

    @Transactional
    public ArtworkOrder createOrder(OrderCommand command, String user, String idempotencyKey, String correlationId) {
        Optional<ArtworkOrder> replay = orders.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) return replay.get();
        ArtworkOffer offer = offers.locked(command.offerId()).orElseThrow();
        if (offer.status != ArtworkOffer.Status.ACTIVE) throw new IllegalStateException("Offer is not active");
        if (offer.saleType != ArtworkOffer.SaleType.FIXED_PRICE) {
            throw new IllegalStateException("Offer requires artisan request workflow");
        }
        if (command.quantity() < 1 || offer.availableQuantity - offer.reservedQuantity < command.quantity()) {
            throw new IllegalStateException("Artwork inventory unavailable");
        }
        if (command.deliveryType() == ArtworkOrder.DeliveryType.INTERNATIONAL_SHIPPING && !offer.internationalShipping) {
            throw new IllegalArgumentException("International shipping unavailable");
        }
        offer.reservedQuantity += command.quantity();
        offers.save(offer);
        CommerceOrder financial = commerce.create(new CommerceService.Create(user, offer.artisanPartnerId,
                offer.artworkId.toString(), ProductType.ARTWORK_ORDER, offer.currencyCode,
                List.of(new CommerceService.Line(offer.artworkId.toString(), "Artwork", command.quantity(), offer.amount)),
                null, BigDecimal.ZERO, BigDecimal.ZERO), "artwork:" + idempotencyKey);
        ArtworkOrder order = new ArtworkOrder();
        order.id = UUID.randomUUID();
        order.reference = "ART-" + order.id.toString().substring(0, 8).toUpperCase();
        order.idempotencyKey = idempotencyKey;
        order.buyerUserId = user;
        order.artisanPartnerId = offer.artisanPartnerId;
        order.offerId = offer.id;
        order.commerceOrderId = financial.id;
        order.status = ArtworkOrder.Status.AWAITING_PAYMENT;
        order.deliveryType = command.deliveryType();
        order.quantity = command.quantity();
        order.grossAmount = financial.totalAmount;
        order.commissionAmount = financial.commissionAmount;
        order.artisanAmount = financial.partnerNetAmount;
        order.currencyCode = financial.currency;
        orders.save(order);
        record(order, null, order.status, "created", user);
        event("ArtworkOrderCreated", order, correlationId, offer.artworkId);
        return order;
    }

    @Transactional
    public void paymentSucceeded(UUID commerceOrderId) {
        paymentSucceeded(commerceOrderId, commerceOrderId.toString());
    }

    @Transactional
    public void paymentSucceeded(UUID commerceOrderId, String correlationId) {
        ArtworkOrder order = orders.findByCommerceOrderId(commerceOrderId).orElse(null);
        if (order == null || order.status == ArtworkOrder.Status.PAID) return;
        transition(order, ArtworkOrder.Status.PAID, "payment succeeded", "commerce-service");
        ArtworkOffer offer = offers.locked(order.offerId).orElseThrow();
        offer.reservedQuantity -= order.quantity;
        offer.availableQuantity -= order.quantity;
        if (offer.availableQuantity == 0) offer.status = ArtworkOffer.Status.SOLD_OUT;
        offers.save(offer);
        event("ArtworkOrderPaid", order, correlationId, offer.artworkId);
        event("ArtworkSold", order, correlationId, offer.artworkId);
    }

    @Transactional
    public void paymentFailed(UUID commerceOrderId) {
        paymentFailed(commerceOrderId, commerceOrderId.toString());
    }

    @Transactional
    public void paymentFailed(UUID commerceOrderId, String correlationId) {
        ArtworkOrder order = orders.findByCommerceOrderId(commerceOrderId).orElse(null);
        if (order == null || order.status != ArtworkOrder.Status.AWAITING_PAYMENT) return;
        release(order);
        transition(order, ArtworkOrder.Status.CANCELLED, "payment failed", "commerce-service");
        event("ArtworkOrderCancelled", order, correlationId, null);
    }

    @Transactional
    public void refunded(UUID commerceOrderId) {
        refunded(commerceOrderId, commerceOrderId.toString());
    }

    @Transactional
    public void refunded(UUID commerceOrderId, String correlationId) {
        ArtworkOrder order = orders.findByCommerceOrderId(commerceOrderId).orElse(null);
        if (order == null || order.status == ArtworkOrder.Status.REFUNDED) return;
        if (order.status == ArtworkOrder.Status.CANCELLED || order.status == ArtworkOrder.Status.AWAITING_PAYMENT) {
            throw new IllegalStateException("Order is not refundable");
        }
        transition(order, ArtworkOrder.Status.REFUNDED, "payment refunded", "commerce-service");
        event("ArtworkOrderRefunded", order, correlationId, null);
    }

    @Transactional
    public ArtworkOrder cancel(UUID id, String user, String reason) {
        return cancel(id, user, reason, id.toString());
    }

    @Transactional
    public ArtworkOrder cancel(UUID id, String user, String reason, String correlationId) {
        ArtworkOrder order = orders.locked(id).orElseThrow();
        if (!order.buyerUserId.equals(user)) throw new SecurityException("Order does not belong to buyer");
        if (order.status != ArtworkOrder.Status.AWAITING_PAYMENT && order.status != ArtworkOrder.Status.PENDING) {
            throw new IllegalStateException("Paid order requires refund workflow");
        }
        release(order);
        transition(order, ArtworkOrder.Status.CANCELLED, reason, user);
        event("ArtworkOrderCancelled", order, correlationId, null);
        return order;
    }

    @Transactional
    public ArtworkOrder artisanStatus(UUID id, ArtworkOrder.Status target, String partner, boolean admin, String reason) {
        return artisanStatus(id, target, partner, admin, reason, id.toString());
    }

    @Transactional
    public ArtworkOrder artisanStatus(UUID id, ArtworkOrder.Status target, String partner, boolean admin, String reason,
            String correlationId) {
        ArtworkOrder order = orders.locked(id).orElseThrow();
        requirePartner(order.artisanPartnerId, partner, admin);
        Set<ArtworkOrder.Status> allowed = switch (order.status) {
            case PAID -> Set.of(ArtworkOrder.Status.ACCEPTED, ArtworkOrder.Status.CANCELLED);
            case ACCEPTED -> Set.of(ArtworkOrder.Status.IN_PRODUCTION, ArtworkOrder.Status.READY);
            case IN_PRODUCTION -> Set.of(ArtworkOrder.Status.READY);
            case READY -> Set.of(ArtworkOrder.Status.SHIPPED, ArtworkOrder.Status.DELIVERED);
            case SHIPPED -> Set.of(ArtworkOrder.Status.DELIVERED);
            default -> Set.of();
        };
        if (!allowed.contains(target)) throw new IllegalStateException("Invalid artwork order transition");
        transition(order, target, reason, partner);
        event("ArtworkOrder" + camel(target), order, correlationId, null);
        return order;
    }

    @Transactional(readOnly = true)
    public List<ArtworkOrder> mine(String user) { return orders.findByBuyerUserIdOrderByCreatedAtDesc(user); }

    @Transactional(readOnly = true)
    public List<ArtworkOrder> artisan(String partner) { return orders.findByArtisanPartnerIdOrderByCreatedAtDesc(partner); }

    @Transactional(readOnly = true)
    public ArtworkOrder detail(UUID id, String actor, boolean admin) {
        ArtworkOrder order = orders.findById(id).orElseThrow();
        if (!admin && !order.buyerUserId.equals(actor) && !order.artisanPartnerId.equals(actor)) {
            throw new SecurityException("Order access denied");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<ArtworkOffer> offer(UUID artworkId) { return offers.findByArtworkId(artworkId); }

    private void validateOffer(OfferCommand command) {
        if (command.availableQuantity() < 0) throw new IllegalArgumentException("Invalid quantity");
        if (command.saleType() == ArtworkOffer.SaleType.FIXED_PRICE
                && (command.amount() == null || command.amount().signum() <= 0 || command.currencyCode() == null)) {
            throw new IllegalArgumentException("Fixed price and currency required");
        }
        if (command.saleType() == ArtworkOffer.SaleType.AUCTION_FUTURE && command.status() == ArtworkOffer.Status.ACTIVE) {
            throw new IllegalArgumentException("Auction is not implemented");
        }
    }

    private void apply(ArtworkOffer offer, OfferCommand command) {
        offer.artworkId = command.artworkId(); offer.artisanPartnerId = command.artisanPartnerId();
        offer.saleType = command.saleType(); offer.amount = command.amount();
        offer.currencyCode = command.currencyCode() == null ? null : command.currencyCode().toUpperCase(Locale.ROOT);
        offer.availableQuantity = command.availableQuantity(); offer.countryCode = command.countryCode();
        offer.internationalShipping = command.internationalShipping(); offer.customOrderAllowed = command.customOrderAllowed();
        offer.status = command.status();
    }

    private void release(ArtworkOrder order) {
        ArtworkOffer offer = offers.locked(order.offerId).orElseThrow();
        offer.reservedQuantity = Math.max(0, offer.reservedQuantity - order.quantity);
        offers.save(offer);
    }

    private void transition(ArtworkOrder order, ArtworkOrder.Status target, String reason, String actor) {
        ArtworkOrder.Status old = order.status;
        order.status = target;
        orders.save(order);
        record(order, old, target, reason, actor);
    }

    private void record(ArtworkOrder order, ArtworkOrder.Status old, ArtworkOrder.Status target, String reason, String actor) {
        ArtworkOrderHistory historyEntry = new ArtworkOrderHistory();
        historyEntry.id = UUID.randomUUID(); historyEntry.orderId = order.id;
        historyEntry.previousStatus = old == null ? null : old.name(); historyEntry.newStatus = target.name();
        historyEntry.reason = reason; historyEntry.actorId = actor; historyEntry.createdAt = Instant.now();
        history.save(historyEntry);
    }

    private void event(String type, ArtworkOrder order, String correlationId, UUID artworkId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.id); payload.put("artworkOfferId", order.offerId);
        if (artworkId != null) payload.put("artworkId", artworkId);
        payload.put("buyerUserId", order.buyerUserId); payload.put("artisanPartnerId", order.artisanPartnerId);
        payload.put("status", order.status.name()); payload.put("amount", order.grossAmount); payload.put("currency", order.currencyCode);
        outbox.append("commerce.events", type, order.id.toString(),
                correlationId == null || correlationId.isBlank() ? order.id.toString() : correlationId, payload);
    }

    private void requirePartner(String expected, String actor, boolean admin) {
        if (!admin && !expected.equals(actor)) throw new SecurityException("Partner access denied");
    }

    private String camel(ArtworkOrder.Status status) {
        String value = status.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
