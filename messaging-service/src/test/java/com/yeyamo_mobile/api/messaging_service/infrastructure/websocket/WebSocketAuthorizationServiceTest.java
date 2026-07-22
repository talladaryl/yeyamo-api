package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeyamo_mobile.api.messaging_service.domain.MemberStatus;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationMemberEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationMemberRepository;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthorizationServiceTest {

    @Mock
    private ConversationMemberRepository memberRepository;

    private WebSocketAuthorizationService service;

    private UUID conversationId;
    private String userId;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        service = new WebSocketAuthorizationService(memberRepository);
        conversationId = UUID.randomUUID();
        userId = "user-123";
        otherUserId = "user-456";
    }

    // ─── TESTS AUTORISATION MEMBRE ACTIF ────────────────────────────────────────

    @Test
    void shouldAuthorizeActiveMember() {
        String destination = "/user/queue/conversation." + conversationId;
        ConversationMemberEntity member = createMember(conversationId, userId, MemberStatus.ACTIVE);
        
        when(memberRepository.findByConversationIdAndUserId(conversationId, userId))
            .thenReturn(Optional.of(member));

        boolean authorized = service.isAuthorizedToSubscribe(userId, destination);

        assertTrue(authorized);
    }

    @Test
    void shouldRejectNonMember() {
        String destination = "/user/queue/conversation." + conversationId;
        
        when(memberRepository.findByConversationIdAndUserId(conversationId, otherUserId))
            .thenReturn(Optional.empty());

        boolean authorized = service.isAuthorizedToSubscribe(otherUserId, destination);

        assertFalse(authorized, "Non-member should be rejected");
    }

    @Test
    void shouldRejectLeftMember() {
        String destination = "/user/queue/conversation." + conversationId;
        ConversationMemberEntity member = createMember(conversationId, userId, MemberStatus.LEFT);
        
        when(memberRepository.findByConversationIdAndUserId(conversationId, userId))
            .thenReturn(Optional.of(member));

        boolean authorized = service.isAuthorizedToSubscribe(userId, destination);

        assertFalse(authorized, "LEFT member should be rejected");
    }

    @Test
    void shouldRejectRemovedMember() {
        String destination = "/user/queue/conversation." + conversationId;
        ConversationMemberEntity member = createMember(conversationId, userId, MemberStatus.REMOVED);
        
        when(memberRepository.findByConversationIdAndUserId(conversationId, userId))
            .thenReturn(Optional.of(member));

        boolean authorized = service.isAuthorizedToSubscribe(userId, destination);

        assertFalse(authorized, "REMOVED member should be rejected");
    }

    // ─── TESTS EXTRACTION CONVERSATION ID ───────────────────────────────────────

    @Test
    void shouldExtractConversationIdFromDestination() {
        String destination = "/user/queue/conversation." + conversationId;
        ConversationMemberEntity member = createMember(conversationId, userId, MemberStatus.ACTIVE);
        
        when(memberRepository.findByConversationIdAndUserId(conversationId, userId))
            .thenReturn(Optional.of(member));

        boolean authorized = service.isAuthorizedToSubscribe(userId, destination);

        assertTrue(authorized);
        verify(memberRepository).findByConversationIdAndUserId(conversationId, userId);
    }

    @Test
    void shouldRejectInvalidConversationIdFormat() {
        String destination = "/user/queue/conversation.invalid-uuid";

        boolean authorized = service.isAuthorizedToSubscribe(userId, destination);

        assertFalse(authorized, "Invalid UUID format should be rejected");
        verify(memberRepository, never()).findByConversationIdAndUserId(any(), any());
    }

    // ─── TESTS DESTINATIONS NON-CONVERSATION ─────────────────────────────────────

    @Test
    void shouldAuthorizeGeneralMessagingQueue() {
        String destination = "/user/queue/messaging";

        boolean authorized = service.isAuthorizedToSubscribe(userId, destination);

        assertTrue(authorized, "General messaging queue should be allowed");
        verify(memberRepository, never()).findByConversationIdAndUserId(any(), any());
    }

    @Test
    void shouldAuthorizeNullDestination() {
        boolean authorized = service.isAuthorizedToSubscribe(userId, null);

        assertTrue(authorized, "Null destination should be allowed (handled by other checks)");
        verify(memberRepository, never()).findByConversationIdAndUserId(any(), any());
    }

    // ─── TESTS PROTECTION FUITE D'INFORMATION ───────────────────────────────────

    @Test
    void shouldNotRevealConversationExistence() {
        String destination = "/user/queue/conversation." + conversationId;
        
        // Cas 1: Conversation n'existe pas
        when(memberRepository.findByConversationIdAndUserId(conversationId, userId))
            .thenReturn(Optional.empty());
        boolean result1 = service.isAuthorizedToSubscribe(userId, destination);
        
        // Cas 2: Conversation existe mais utilisateur pas membre
        UUID otherConversationId = UUID.randomUUID();
        String destination2 = "/user/queue/conversation." + otherConversationId;
        when(memberRepository.findByConversationIdAndUserId(otherConversationId, userId))
            .thenReturn(Optional.empty());
        boolean result2 = service.isAuthorizedToSubscribe(userId, destination2);

        // Les deux cas doivent retourner le même résultat (false)
        // Pas de distinction entre "n'existe pas" et "pas autorisé"
        assertFalse(result1);
        assertFalse(result2);
        assertEquals(result1, result2, "Should not reveal conversation existence");
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private ConversationMemberEntity createMember(UUID conversationId, String userId, MemberStatus status) {
        ConversationMemberEntity member = mock(ConversationMemberEntity.class);
        when(member.getStatus()).thenReturn(status);
        return member;
    }
}
