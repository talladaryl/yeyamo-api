package com.yeyamo_mobile.api.messaging_service.application;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.yeyamo_mobile.api.messaging_service.application.port.MessagingEventPort;
import com.yeyamo_mobile.api.messaging_service.domain.ConversationType;
import com.yeyamo_mobile.api.messaging_service.domain.MessagingException;
import com.yeyamo_mobile.api.messaging_service.infrastructure.client.PartnerIdentityClient;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
class PartnerConversationIntegrationTest {

    @Autowired
    private MessagingApplicationService service;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private DirectConversationRepository directConversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageIdempotencyRepository messageIdempotencyRepository;

    @MockitoBean
    private PartnerIdentityClient partnerIdentityClient;

    @MockitoBean
    private MessagingEventPort events;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @BeforeEach
    void setUp() {
        messageIdempotencyRepository.deleteAll();
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        directConversationRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    @DisplayName("Devrait créer une conversation directe avec l'utilisateur du partenaire résolu")
    void shouldCreatePartnerConversationSuccessfully() {
        UUID partnerId = UUID.randomUUID();
        String artisanUserId = "artisan-user-uuid-999";
        String clientUserId = "client-user-111";

        when(partnerIdentityClient.resolveUserId(partnerId)).thenReturn(artisanUserId);

        var conv = service.createPartnerConversation(clientUserId, partnerId, "corr-test-1");

        assertThat(conv).isNotNull();
        assertThat(conv.id()).isNotNull();
        assertThat(conv.type()).isEqualTo(ConversationType.DIRECT);
        assertThat(conv.members()).hasSize(2);
        assertThat(conv.members().stream().map(m -> m.userId()).toList())
                .containsExactlyInAnyOrder(clientUserId, artisanUserId);

        // Vérification de la paire idempotente
        String pairKey = clientUserId.compareTo(artisanUserId) <= 0 
                ? clientUserId + "#" + artisanUserId 
                : artisanUserId + "#" + clientUserId;
        assertThat(directConversationRepository.findById(pairKey)).isPresent();
    }

    @Test
    @DisplayName("Devrait être idempotent : appel répété retourne la même conversation directe")
    void shouldReturnSameConversationOnRepeatedCall() {
        UUID partnerId = UUID.randomUUID();
        String artisanUserId = "artisan-user-uuid-888";
        String clientUserId = "client-user-222";

        when(partnerIdentityClient.resolveUserId(partnerId)).thenReturn(artisanUserId);

        var conv1 = service.createPartnerConversation(clientUserId, partnerId, "corr-test-2a");
        var conv2 = service.createPartnerConversation(clientUserId, partnerId, "corr-test-2b");

        assertThat(conv1.id()).isEqualTo(conv2.id());
        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Devrait propager PARTNER_NOT_FOUND quand le client lève une 404")
    void shouldPropagatePartnerNotFound() {
        UUID partnerId = UUID.randomUUID();

        when(partnerIdentityClient.resolveUserId(partnerId))
                .thenThrow(new MessagingException("PARTNER_NOT_FOUND", "Partenaire introuvable ou indisponible"));

        assertThatThrownBy(() -> service.createPartnerConversation("client-user-333", partnerId, "corr-test-3"))
                .isInstanceOf(MessagingException.class)
                .satisfies(ex -> assertThat(((MessagingException) ex).getCode()).isEqualTo("PARTNER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Devrait propager PARTNER_RESOLUTION_FAILED en cas d'erreur ou timeout")
    void shouldPropagatePartnerResolutionFailed() {
        UUID partnerId = UUID.randomUUID();

        when(partnerIdentityClient.resolveUserId(partnerId))
                .thenThrow(new MessagingException("PARTNER_RESOLUTION_FAILED", "Impossible de contacter ce partenaire pour le moment"));

        assertThatThrownBy(() -> service.createPartnerConversation("client-user-444", partnerId, "corr-test-4"))
                .isInstanceOf(MessagingException.class)
                .satisfies(ex -> assertThat(((MessagingException) ex).getCode()).isEqualTo("PARTNER_RESOLUTION_FAILED"));
    }
}
