package com.yeyamo_mobile.api.messaging_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.CreateConversation;
import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.SendMessage;
import com.yeyamo_mobile.api.messaging_service.application.port.MessagingEventPort;
import com.yeyamo_mobile.api.messaging_service.domain.ConversationType;
import com.yeyamo_mobile.api.messaging_service.domain.MemberRole;
import com.yeyamo_mobile.api.messaging_service.domain.MessageType;
import com.yeyamo_mobile.api.messaging_service.domain.MessagingException;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationByUserEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationByUserRepository;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationMemberEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationMemberRepository;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationRepository;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.DirectConversationEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.DirectConversationRepository;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.MessageByIdEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.MessageByIdRepository;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.MessageEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.MessageIdempotencyEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.MessageIdempotencyRepository;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.MessageRepository;

class MessagingApplicationServiceTest {

    private ConversationRepository conversations;
    private ConversationMemberRepository members;
    private ConversationByUserRepository byUser;
    private DirectConversationRepository direct;
    private MessageRepository messages;
    private MessageByIdRepository messageIds;
    private MessageIdempotencyRepository idempotency;
    private MessagingEventPort events;
    private MessagingApplicationService service;

    @BeforeEach
    void setUp() {
        conversations = mock(ConversationRepository.class);
        members = mock(ConversationMemberRepository.class);
        byUser = mock(ConversationByUserRepository.class);
        direct = mock(DirectConversationRepository.class);
        messages = mock(MessageRepository.class);
        messageIds = mock(MessageByIdRepository.class);
        idempotency = mock(MessageIdempotencyRepository.class);
        events = mock(MessagingEventPort.class);
        when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(members.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(byUser.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageIds.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new MessagingApplicationService(conversations, members, byUser, direct, messages,
                messageIds, idempotency, events, 100, 4_000, 10, 15);
    }

    @Test
    void createsAUniqueDirectConversationForTheUserPair() {
        when(direct.findById("alice#bob")).thenReturn(Optional.empty());

        var result = service.create("alice",
                new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-1");

        assertEquals(ConversationType.DIRECT, result.type());
        assertEquals(2, result.members().size());
        verify(direct).save(any(DirectConversationEntity.class));
        verify(events).publish(eq("messaging.conversation.created"), eq(result.id().toString()),
                eq("alice"), eq("corr-1"), eq(Set.of("bob")), any(), any());
    }

    @Test
    void sendsAnIdempotentMessageAndNotifiesOnlyOtherMembers() {
        ConversationEntity conversation = ConversationEntity.create(ConversationType.DIRECT, null, "alice");
        ConversationMemberEntity alice = ConversationMemberEntity.active(conversation.getId(), "alice", MemberRole.OWNER);
        ConversationMemberEntity bob = ConversationMemberEntity.active(conversation.getId(), "bob", MemberRole.MEMBER);
        when(members.findByConversationIdAndUserId(conversation.getId(), "alice")).thenReturn(Optional.of(alice));
        when(members.findByConversationId(conversation.getId())).thenReturn(List.of(alice, bob));
        when(idempotency.findBySenderIdAndClientMessageId("alice", "client-1")).thenReturn(Optional.empty());
        when(conversations.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        var result = service.send("alice", conversation.getId(),
                new SendMessage("client-1", MessageType.TEXT, "Bonjour", List.of(), null), "corr-2");

        assertEquals("Bonjour", result.body());
        verify(idempotency).save(any(MessageIdempotencyEntity.class));
        verify(events).publish(eq("messaging.message.sent"), eq(conversation.getId().toString()),
                eq("alice"), eq("corr-2"), eq(List.of("bob")), any(), eq(result));
    }

    @Test
    void rejectsAUserWhoIsNotAnActiveMember() {
        UUID conversationId = UUID.randomUUID();
        when(members.findByConversationIdAndUserId(conversationId, "intruder")).thenReturn(Optional.empty());

        MessagingException error = assertThrows(MessagingException.class,
                () -> service.send("intruder", conversationId,
                        new SendMessage("client-2", MessageType.TEXT, "Bonjour", List.of(), null), "corr-3"));

        assertEquals("FORBIDDEN", error.getCode());
    }

    @Test
    void replaysTheSameMessageForTheSameClientIdentifier() {
        ConversationEntity conversation = ConversationEntity.create(ConversationType.DIRECT, null, "alice");
        ConversationMemberEntity alice = ConversationMemberEntity.active(conversation.getId(), "alice", MemberRole.OWNER);
        ConversationMemberEntity bob = ConversationMemberEntity.active(conversation.getId(), "bob", MemberRole.MEMBER);
        MessageEntity original = MessageEntity.create(conversation.getId(), "alice", "client-3",
                MessageType.TEXT, "Déjà envoyé", List.of(), null);
        MessageByIdEntity stored = MessageByIdEntity.from(original);
        when(members.findByConversationIdAndUserId(conversation.getId(), "alice")).thenReturn(Optional.of(alice));
        when(members.findByConversationId(conversation.getId())).thenReturn(List.of(alice, bob));
        when(idempotency.findBySenderIdAndClientMessageId("alice", "client-3"))
                .thenReturn(Optional.of(new MessageIdempotencyEntity("alice", "client-3", original.getMessageId(), conversation.getId())));
        when(messageIds.findById(original.getMessageId())).thenReturn(Optional.of(stored));

        var replay = service.send("alice", conversation.getId(),
                new SendMessage("client-3", MessageType.TEXT, "Autre contenu", List.of(), null), "corr-4");

        assertEquals(original.getMessageId(), replay.id());
        assertEquals("Déjà envoyé", replay.body());
        ArgumentCaptor<java.util.Map<String, Object>> payload = ArgumentCaptor.forClass(java.util.Map.class);
        verify(events).publish(eq("messaging.message.sent"), eq(conversation.getId().toString()),
                eq("alice"), eq("corr-4"), eq(List.of("bob")), payload.capture(), eq(replay));
        assertTrue(payload.getValue().containsKey("messageId"));
    }
}
