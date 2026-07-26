package com.yeyamo_mobile.api.ticket_service.infrastructure.crypto;

import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.SpringTicketQrCredentialRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketEntity;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketQrCredentialEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QrTokenServiceTest {

    @Test
    void validatesIssuedTokenAndRejectsTampering() {
        SpringTicketQrCredentialRepository repository =
            mock(SpringTicketQrCredentialRepository.class);
        AtomicReference<TicketQrCredentialEntity> stored = new AtomicReference<>();
        when(repository.findByTicketId(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            TicketQrCredentialEntity credential = invocation.getArgument(0);
            stored.set(credential);
            return credential;
        });
        when(repository.findByTokenId(any())).thenAnswer(invocation -> {
            TicketQrCredentialEntity credential = stored.get();
            return credential != null && credential.getTokenId().equals(invocation.getArgument(0))
                ? Optional.of(credential) : Optional.empty();
        });

        QrTokenService service = new QrTokenService(repository, 30);
        TicketEntity ticket = new TicketEntity();
        ticket.setId(UUID.randomUUID());
        ticket.setEventId("event-1");

        String token = service.generateQrToken(ticket);

        assertThat(service.validateQrToken(token, "event-1").isValid()).isTrue();
        assertThat(service.validateQrToken(token, "event-2").getErrorCode())
            .isEqualTo("WRONG_EVENT");

        String tampered = token.substring(0, token.length() - 1)
            + (token.endsWith("A") ? "B" : "A");
        assertThat(service.validateQrToken(tampered, "event-1").getErrorCode())
            .isEqualTo("INVALID");
    }
}
