package com.yeyamo_mobile.api.ticket_service.application;

import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock SpringTicketHoldRepository holdRepository;
    @Mock SpringTicketTypeRepository ticketTypeRepository;
    @Mock SpringTicketSaleConfigurationRepository configurationRepository;
    @Mock SpringTicketRepository ticketRepository;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(
            holdRepository, ticketTypeRepository, configurationRepository, ticketRepository, 10
        );
    }

    @Test
    void rejectsTicketTypeFromAnotherEventBeforeReservingInventory() {
        UUID typeId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();
        TicketTypeEntity type = activeType(typeId, configurationId);
        TicketSaleConfigurationEntity configuration = activeConfiguration("event-a");

        when(holdRepository.findByUserIdAndIdempotencyKey("user-1", "request-1"))
            .thenReturn(Optional.empty());
        when(ticketTypeRepository.findByIdForUpdate(typeId)).thenReturn(Optional.of(type));
        when(configurationRepository.findById(configurationId))
            .thenReturn(Optional.of(configuration));

        assertThatThrownBy(() -> service.createHold(new InventoryService.CreateHoldRequest(
            "user-1", "event-b", typeId, 1, "request-1"
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong");

        verify(ticketTypeRepository, never()).save(any());
        verify(holdRepository, never()).save(any());
    }

    @Test
    void enforcesMaximumTicketsPerBuyer() {
        UUID typeId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();
        TicketTypeEntity type = activeType(typeId, configurationId);
        TicketSaleConfigurationEntity configuration = activeConfiguration("event-a");
        configuration.setMaxTicketsPerBuyer(4);

        when(holdRepository.findByUserIdAndIdempotencyKey("user-1", "request-1"))
            .thenReturn(Optional.empty());
        when(ticketTypeRepository.findByIdForUpdate(typeId)).thenReturn(Optional.of(type));
        when(configurationRepository.findById(configurationId))
            .thenReturn(Optional.of(configuration));
        when(holdRepository.sumReservedByUserAndEvent(eq("user-1"), eq("event-a"), any()))
            .thenReturn(2L);
        when(ticketRepository.countOwnedTicketsForEvent("user-1", "event-a")).thenReturn(2L);

        assertThatThrownBy(() -> service.createHold(new InventoryService.CreateHoldRequest(
            "user-1", "event-a", typeId, 1, "request-1"
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum");
    }

    private TicketTypeEntity activeType(UUID id, UUID configurationId) {
        TicketTypeEntity type = new TicketTypeEntity();
        type.setId(id);
        type.setSaleConfigurationId(configurationId);
        type.setStatus("ACTIVE");
        type.setQuantityTotal(10);
        type.setQuantityReserved(0);
        type.setQuantitySold(0);
        return type;
    }

    private TicketSaleConfigurationEntity activeConfiguration(String eventId) {
        TicketSaleConfigurationEntity configuration = new TicketSaleConfigurationEntity();
        configuration.setEventId(eventId);
        configuration.setStatus("ACTIVE");
        configuration.setSalesStartAt(Instant.now().minusSeconds(60));
        configuration.setSalesEndAt(Instant.now().plusSeconds(3600));
        configuration.setMaxTicketsPerBuyer(10);
        return configuration;
    }
}
