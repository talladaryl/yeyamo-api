package com.yeyamo_mobile.api.commerce_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.commerce_service.application.CommerceService;
import com.yeyamo_mobile.api.commerce_service.application.ArtworkCommerceService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class PaymentEventConsumerTest {
    private final CommerceService commerce = mock(CommerceService.class);
    private final ArtworkCommerceService artworks = mock(ArtworkCommerceService.class);
    private final ProcessedPaymentEventRepository processed =
        mock(ProcessedPaymentEventRepository.class);
    private final PaymentEventConsumer consumer =
        new PaymentEventConsumer(commerce, artworks, new ObjectMapper(), processed);

    @Test
    void duplicateProviderCallbackIsIgnored() throws Exception {
        String eventId = UUID.randomUUID().toString();
        when(processed.existsById(eventId)).thenReturn(false, true);
        String event = envelope(eventId, "payment.confirmed",
            UUID.randomUUID(), "client-order", "\"paymentId\":\"pay-1\"");

        consumer.consume(event);
        consumer.consume(event);

        verify(commerce, times(1)).paid(any(), eq("pay-1"));
        verify(artworks, times(1)).paymentSucceeded(any(), any());
        verify(processed, times(1)).save(any());
    }

    @Test
    void refundCallbackUsesRefundCorrelationId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        when(processed.existsById(eventId)).thenReturn(false);

        consumer.consume(envelope(eventId, "payment.refunded", orderId,
            refundId.toString(), "\"refundId\":\"provider-refund-7\""));

        verify(commerce).refundCompleted(
            orderId, refundId, "provider-refund-7");
        verify(artworks).refunded(eq(orderId), any());
    }

    private String envelope(String eventId, String type, UUID orderId,
            String correlationId, String payloadFields) {
        return """
            {"eventId":"%s","eventType":"%s","eventVersion":1,
             "correlationId":"%s",
             "payload":{"bookingId":"%s",%s}}
            """.formatted(eventId, type, correlationId, orderId, payloadFields);
    }
}
