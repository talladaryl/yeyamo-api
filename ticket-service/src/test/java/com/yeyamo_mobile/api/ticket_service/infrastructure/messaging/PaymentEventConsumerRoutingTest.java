package com.yeyamo_mobile.api.ticket_service.infrastructure.messaging;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ticket_service.application.InventoryService;
import com.yeyamo_mobile.api.ticket_service.application.TicketIssuanceService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;

class PaymentEventConsumerRoutingTest {
    @Test
    void commercePaymentDoesNotIssueTickets() throws Exception {
        SpringTicketOrderRepository orders = mock(SpringTicketOrderRepository.class);
        SpringTicketHoldRepository holds = mock(SpringTicketHoldRepository.class);
        TicketIssuanceService issuance = mock(TicketIssuanceService.class);
        InventoryService inventory = mock(InventoryService.class);
        TicketProcessedEventRepository processed = mock(TicketProcessedEventRepository.class);
        var consumer = new PaymentEventConsumer(new ObjectMapper(), orders, holds, issuance, inventory, processed);
        consumer.consume("{\"eventId\":\"11111111-1111-1111-1111-111111111111\",\"eventType\":\"payment.authorized\",\"eventVersion\":1,\"payload\":{\"sourceType\":\"COMMERCE_ORDER\",\"sourceId\":\"22222222-2222-2222-2222-222222222222\",\"paymentId\":\"pay-1\"}}");
        verifyNoInteractions(orders, holds, issuance, inventory);
        verify(processed).save(any());
    }
}
