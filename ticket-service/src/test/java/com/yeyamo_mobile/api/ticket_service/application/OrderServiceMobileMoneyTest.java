package com.yeyamo_mobile.api.ticket_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.ticket_service.infrastructure.outbox.OutboxService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.SpringTicketHoldRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.SpringTicketOrderRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.SpringTicketSaleConfigurationRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.SpringTicketTypeRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketHoldEntity;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketOrderEntity;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketSaleConfigurationEntity;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketTypeEntity;

class OrderServiceMobileMoneyTest {

    private final SpringTicketOrderRepository orders = mock(SpringTicketOrderRepository.class);
    private final SpringTicketHoldRepository holds = mock(SpringTicketHoldRepository.class);
    private final SpringTicketTypeRepository ticketTypes = mock(SpringTicketTypeRepository.class);
    private final SpringTicketSaleConfigurationRepository configurations = mock(SpringTicketSaleConfigurationRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private OrderService service;
    private TicketHoldEntity hold;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, holds, ticketTypes, configurations, outbox, new BigDecimal("5.0"));
        hold = new TicketHoldEntity();
        hold.setId(UUID.randomUUID());
        hold.setUserId("user-1");
        hold.setEventId("event-1");
        hold.setTicketTypeId(UUID.randomUUID());
        hold.setQuantity(2);
        hold.setStatus("ACTIVE");
        hold.setExpiresAt(Instant.now().plusSeconds(600));
        when(holds.findById(hold.getId())).thenReturn(Optional.of(hold));

        TicketTypeEntity type = new TicketTypeEntity();
        type.setId(hold.getTicketTypeId());
        type.setSaleConfigurationId(UUID.randomUUID());
        type.setPrice(new BigDecimal("2500"));
        when(ticketTypes.findById(type.getId())).thenReturn(Optional.of(type));

        TicketSaleConfigurationEntity configuration = new TicketSaleConfigurationEntity();
        configuration.setId(type.getSaleConfigurationId());
        configuration.setPartnerId("partner-1");
        configuration.setCurrency("XOF");
        when(configurations.findById(configuration.getId())).thenReturn(Optional.of(configuration));
        when(orders.save(any(TicketOrderEntity.class))).thenAnswer(invocation -> {
            TicketOrderEntity order = invocation.getArgument(0);
            if (order.getId() == null) order.setId(UUID.randomUUID());
            return order;
        });
    }

    @Test
    void paidTicketOrderPublishesMobileMoneyContract() {
        TicketOrderEntity order = service.createOrder(new OrderService.CreateOrderRequest(
                hold.getId(), "user-1", null, "orange", "+2250701234567", "CI"));

        assertEquals("orange", order.getPaymentOperator());
        assertEquals("+2250701234567", order.getPaymentPhoneNumber());
        assertEquals("CI", order.getPaymentCountryCode());
        verify(outbox).publishPaymentRequested(eq(order.getId().toString()), eq("user-1"), eq("partner-1"),
                eq(order.getTotalAmount()), eq("XOF"), eq("orange"), eq("+2250701234567"), eq("CI"), anyMap());
    }

    @Test
    void ticketOrderWithoutMobileMoneyDetailsIsRejectedBeforeCreatingOrder() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(new OrderService.CreateOrderRequest(
                        hold.getId(), "user-1", null, null, null, "CI")));

        assertEquals("A supported mobile money operator is required", error.getMessage());
        verify(orders, never()).save(any(TicketOrderEntity.class));
    }
}
