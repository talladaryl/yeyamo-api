package com.yeyamo_mobile.api.ticket_service.interfaces.rest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.yeyamo_mobile.api.ticket_service.application.InventoryService;
import com.yeyamo_mobile.api.ticket_service.application.OrderService;
import com.yeyamo_mobile.api.ticket_service.application.TicketIssuanceService;

class UserTicketControllerCountryClaimTest {

    @Test
    void legacyJwtWithoutCountryRequiresRefreshBeforePaidOrder() {
        UserTicketController controller = new UserTicketController(
                mock(InventoryService.class), mock(OrderService.class), mock(TicketIssuanceService.class));
        Jwt legacyJwt = new Jwt("legacy-token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "user-1"));

        assertThrows(TokenRefreshRequiredException.class, () -> controller.createOrder(
                new UserTicketController.CreateOrderRequest(UUID.randomUUID(), null, "mtn", "+237690123456"),
                new JwtAuthenticationToken(legacyJwt)));
    }
}
