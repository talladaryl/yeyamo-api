package com.yeyamo_mobile.api.commerce_service.application;

import static com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.yeyamo_mobile.api.commerce_service.messaging.OutboxService;
import com.yeyamo_mobile.api.commerce_service.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CommercePricingSecurityTest {
    @Test
    void forgedLegacyMoneyIsIgnoredAndCanonicalSnapshotIsStored() {
        Fixture fixture = new Fixture(new BigDecimal("10000"), "XAF", ArtworkOffer.Status.ACTIVE);
        CommerceOrder order = fixture.service.create(fixture.command(new BigDecimal("1"), "USD",
            BigDecimal.ZERO, BigDecimal.ZERO), "key-1");

        assertEquals(new BigDecimal("10000"), order.subtotal);
        assertEquals(new BigDecimal("10000"), order.totalAmount);
        assertEquals(BigDecimal.ZERO.setScale(0), order.taxAmount);
        assertEquals(BigDecimal.ZERO.setScale(0), order.serviceFee);
        assertEquals("XAF", order.currency);
        assertEquals(new BigDecimal("10000"), fixture.savedLine.unitPrice);
        assertTrue(fixture.savedLine.priceSnapshot.contains("artwork_offer:"));
    }

    @Test
    void publishedPriceChangesOnlyAffectNewOrders() {
        Fixture fixture = new Fixture(new BigDecimal("10000"), "XAF", ArtworkOffer.Status.ACTIVE);
        CommerceOrder first = fixture.service.create(fixture.command(null, null, null, null), "first");
        fixture.offer.amount = new BigDecimal("15000");
        CommerceOrder second = fixture.service.create(fixture.command(null, null, null, null), "second");
        assertEquals(new BigDecimal("10000"), first.totalAmount);
        assertEquals(new BigDecimal("15000"), second.totalAmount);
    }

    @Test
    void unpublishedAndUnsupportedSourcesAreRejected() {
        Fixture fixture = new Fixture(BigDecimal.TEN, "XAF", ArtworkOffer.Status.DRAFT);
        assertThrows(IllegalStateException.class, () -> fixture.service.create(fixture.command(null, null, null, null), "draft"));
        var unsupported = new CommerceService.Create("user", null, UUID.randomUUID().toString(), ProductType.BOOKING_ORDER,
            null, List.of(new CommerceService.Line("resource", null, 1, BigDecimal.ONE)), null, null, null, null, cashIn());
        assertEquals("UNSUPPORTED_COMMERCE_SOURCE", assertThrows(IllegalArgumentException.class,
            () -> fixture.service.create(unsupported, "unsupported")).getMessage());
    }

    @Test
    void freeCanonicalSourceDoesNotRequestPayment() {
        Fixture fixture = new Fixture(BigDecimal.ZERO, "XAF", ArtworkOffer.Status.ACTIVE);
        CommerceOrder order = fixture.service.create(fixture.command(BigDecimal.TEN, "USD", BigDecimal.TEN, BigDecimal.TEN), "free");
        assertEquals(OrderStatus.COMPLETED, order.status);
        verify(fixture.outbox, never()).append(eq("payment.commands"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void paidArtworkRequiresValidCashInDetailsAndEmitsCanonicalPaymentCommand() {
        Fixture fixture = new Fixture(new BigDecimal("10000"), "XAF", ArtworkOffer.Status.ACTIVE);
        assertEquals("PAYMENT_DETAILS_REQUIRED", assertThrows(IllegalArgumentException.class,
            () -> fixture.service.create(fixture.command(null, null, null, null, null), "missing")).getMessage());
        assertEquals("PAYMENT_OPERATOR_REQUIRED", assertThrows(IllegalArgumentException.class,
            () -> fixture.service.create(fixture.command(null, null, null, null,
                new CommerceService.CashInDetails("unsupported", "CM", "+237690123456")), "operator")).getMessage());
        assertEquals("PAYMENT_COUNTRY_REQUIRED", assertThrows(IllegalArgumentException.class,
            () -> fixture.service.create(fixture.command(null, null, null, null,
                new CommerceService.CashInDetails("mtn", "CMA", "+237690123456")), "country")).getMessage());
        assertEquals("PAYMENT_PHONE_REQUIRED", assertThrows(IllegalArgumentException.class,
            () -> fixture.service.create(fixture.command(null, null, null, null,
                new CommerceService.CashInDetails("mtn", "CM", "690123456")), "phone")).getMessage());

        CommerceOrder order = fixture.service.create(fixture.command(BigDecimal.ONE, "USD", BigDecimal.TEN, BigDecimal.TEN,
            new CommerceService.CashInDetails("MTN", "cm", "+237690123456")), "valid");

        assertEquals(new BigDecimal("10000"), order.totalAmount);
        assertEquals("XAF", order.currency);
        assertEquals("mtn", order.paymentOperator);
        assertEquals("CM", order.paymentCountryCode);
        assertEquals("+237690123456", order.paymentPhoneNumber);
        @SuppressWarnings("unchecked") var payload = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(fixture.outbox).append(eq("payment.commands"), eq("payment.authorization.requested"), anyString(), anyString(), payload.capture());
        assertEquals("COMMERCE_ORDER", payload.getValue().get("sourceType"));
        assertEquals(order.id, payload.getValue().get("sourceId"));
        assertEquals(new BigDecimal("10000"), payload.getValue().get("amount"));
        assertEquals("XAF", payload.getValue().get("currency"));
        assertEquals("mtn", payload.getValue().get("operator"));
        assertEquals("CM", payload.getValue().get("country"));
        assertEquals("+237690123456", payload.getValue().get("phoneNumber"));
    }

    @Test
    void duplicateCheckoutReturnsTheSameOrderWithoutASecondPaymentRequest() {
        Fixture fixture = new Fixture(new BigDecimal("10000"), "XAF", ArtworkOffer.Status.ACTIVE);
        CommerceOrder existing = new CommerceOrder(); existing.id = UUID.randomUUID();
        when(fixture.orders.findByIdempotencyKey("duplicate")).thenReturn(Optional.of(existing));

        assertSame(existing, fixture.service.create(fixture.command(null, null, null, null, cashIn()), "duplicate"));
        verify(fixture.outbox, never()).append(eq("payment.commands"), anyString(), anyString(), anyString(), anyMap());
    }

    private static CommerceService.CashInDetails cashIn() {
        return new CommerceService.CashInDetails("mtn", "CM", "+237690123456");
    }

    private static final class Fixture {
        final OrderRepository orders = mock(OrderRepository.class);
        final OrderLineRepository lines = mock(OrderLineRepository.class);
        final PromotionRepository promotions = mock(PromotionRepository.class);
        final PromotionUsageRepository usages = mock(PromotionUsageRepository.class);
        final CommissionRepository commissions = mock(CommissionRepository.class);
        final LedgerRepository ledger = mock(LedgerRepository.class);
        final CommerceRefundRepository refunds = mock(CommerceRefundRepository.class);
        final InvoiceRepository invoices = mock(InvoiceRepository.class);
        final OutboxService outbox = mock(OutboxService.class);
        final ArtworkCommerceRepositories.Offers offers = mock(ArtworkCommerceRepositories.Offers.class);
        final ArtworkOffer offer = new ArtworkOffer();
        final CommerceService service;
        OrderLine savedLine;

        Fixture(BigDecimal amount, String currency, ArtworkOffer.Status status) {
            offer.id = UUID.randomUUID(); offer.artworkId = UUID.randomUUID(); offer.artisanPartnerId = "partner";
            offer.saleType = ArtworkOffer.SaleType.FIXED_PRICE; offer.amount = amount; offer.currencyCode = currency;
            offer.availableQuantity = 10; offer.status = status;
            when(orders.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(offers.locked(offer.id)).thenReturn(Optional.of(offer));
            when(commissions.active(anyString(), any(), any())).thenReturn(List.of());
            when(invoices.findByOrderId(any())).thenReturn(Optional.empty());
            when(lines.save(any())).thenAnswer(invocation -> { savedLine = invocation.getArgument(0); return savedLine; });
            service = new CommerceService(orders, lines, promotions, usages, commissions, ledger, refunds, invoices,
                outbox, new CanonicalPriceResolver(offers));
        }

        CommerceService.Create command(BigDecimal unitPrice, String currency, BigDecimal tax, BigDecimal fee) {
            return command(unitPrice, currency, tax, fee, cashIn());
        }

        CommerceService.Create command(BigDecimal unitPrice, String currency, BigDecimal tax, BigDecimal fee,
                CommerceService.CashInDetails cashInDetails) {
            return new CommerceService.Create("user", "forged-partner", offer.id.toString(), ProductType.ARTWORK_ORDER,
                currency, List.of(new CommerceService.Line(offer.artworkId.toString(), "forged", 1, unitPrice)),
                offer.id.toString(), null, tax, fee, cashInDetails);
        }
    }
}
