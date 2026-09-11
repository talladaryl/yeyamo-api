package com.yeyamo_mobile.api.commerce_service.application;

import com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.ProductType;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkCommerceRepositories;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkOffer;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CanonicalPriceResolver {
    private final ArtworkCommerceRepositories.Offers artworkOffers;

    public CanonicalPriceResolver(ArtworkCommerceRepositories.Offers artworkOffers) {
        this.artworkOffers = artworkOffers;
    }

    public record Request(ProductType resourceType, String resourceId, int quantity, String authenticatedUser,
            String offerId) { }

    public record Price(String productId, String description, int quantity, BigDecimal unitPrice, String currency,
            String partnerId, String canonicalSource) { }

    public Price resolve(Request request) {
        if (request.quantity() < 1) throw new IllegalArgumentException("Quantity must be positive");
        if (request.resourceType() != ProductType.ARTWORK_ORDER) {
            throw new IllegalArgumentException("UNSUPPORTED_COMMERCE_SOURCE");
        }
        UUID offerId = parseUuid(request.offerId() == null ? request.resourceId() : request.offerId());
        ArtworkOffer offer = artworkOffers.locked(offerId)
            .orElseThrow(() -> new IllegalArgumentException("UNSUPPORTED_COMMERCE_SOURCE"));
        if (offer.status != ArtworkOffer.Status.ACTIVE || offer.saleType != ArtworkOffer.SaleType.FIXED_PRICE) {
            throw new IllegalStateException("Commerce source is not published for sale");
        }
        if (offer.availableQuantity - offer.reservedQuantity < request.quantity()) {
            throw new IllegalStateException("Artwork inventory unavailable");
        }
        if (offer.amount == null || offer.amount.signum() < 0 || offer.currencyCode == null) {
            throw new IllegalStateException("Commerce source has no canonical price");
        }
        return new Price(offer.artworkId.toString(), "Artwork", request.quantity(), offer.amount,
            offer.currencyCode, offer.artisanPartnerId, "artwork_offer:" + offer.id);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("UNSUPPORTED_COMMERCE_SOURCE", exception);
        }
    }
}
