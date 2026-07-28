package com.yeyamo_mobile.api.ticket_service.interfaces.rest;

import com.yeyamo_mobile.api.ticket_service.application.TicketManagementService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketSaleConfigurationEntity;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketTypeEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets/events")
public class PublicTicketTypeController {
    private final TicketManagementService service;
    public PublicTicketTypeController(TicketManagementService service) { this.service = service; }

    @GetMapping("/{eventId}/types")
    public AvailableTicketTypesResponse available(@PathVariable String eventId) {
        TicketSaleConfigurationEntity configuration = service.configurationForEvent(eventId);
        return new AvailableTicketTypesResponse(eventId, configuration.getCurrency(),
            service.availableTypes(eventId).stream().map(PublicTicketTypeController::response).toList());
    }

    private static TicketTypeResponse response(TicketTypeEntity type) {
        return new TicketTypeResponse(type.getId(), type.getCode(), type.getName(), type.getDescription(),
            type.getPrice(), type.getQuantityTotal(), type.getQuantitySold(), type.getAvailableQuantity(),
            type.getStatus(), type.getSalesStartAt(), type.getSalesEndAt(), type.getAccessZone(),
            type.getGateInstructions());
    }

    public record AvailableTicketTypesResponse(String eventId, String currency, List<TicketTypeResponse> tickets) {}
    public record TicketTypeResponse(UUID id, String code, String name, String description,
        BigDecimal price, int quantityTotal, int quantitySold, int quantityAvailable, String status,
        Instant salesStartAt, Instant salesEndAt, String accessZone, String gateInstructions) {}
}
