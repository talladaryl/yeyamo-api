package com.yeyamo_mobile.api.messaging_service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.yeyamo_mobile.api.messaging_service.application.MessagingApplicationService;
import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.*;
import com.yeyamo_mobile.api.messaging_service.application.port.MessagingEventPort;
import com.yeyamo_mobile.api.messaging_service.domain.ConversationType;
import com.yeyamo_mobile.api.messaging_service.domain.MemberRole;
import com.yeyamo_mobile.api.messaging_service.domain.MemberStatus;
import com.yeyamo_mobile.api.messaging_service.domain.MessageType;
import com.yeyamo_mobile.api.messaging_service.domain.MessagingException;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.*;

@SpringBootTest
class MessagingIntegrationTest {

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
    private MessagingEventPort events;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @BeforeEach
    void cleanDatabase() {
        messageIdempotencyRepository.deleteAll();
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        directConversationRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    void test1_CreateDirectConversationAndUniquePair() {
        var conv1 = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-1");
        assertNotNull(conv1.id());
        assertEquals(ConversationType.DIRECT, conv1.type());
        assertEquals(2, conv1.members().size());

        // Inverser la paire (bob -> alice) doit retourner la même conversation existante
        var conv2 = service.create("bob", new CreateConversation(ConversationType.DIRECT, null, Set.of("alice")), "corr-2");
        assertEquals(conv1.id(), conv2.id());

        // Vérification directe en base
        assertTrue(directConversationRepository.findById("alice#bob").isPresent());
    }

    @Test
    void test2_CreateGroupConversationAndMemberManagement() {
        var group = service.create("alice", new CreateConversation(ConversationType.GROUP, "Dev Team", Set.of("bob")), "corr-3");
        assertEquals("Dev Team", group.title());
        assertEquals(2, group.members().size());

        // Ajout d'un membre "carol"
        var updated = service.addMember("alice", group.id(), "carol", "corr-4");
        assertEquals(3, updated.members().size());

        // Retrait d'un membre "carol"
        service.removeMember("alice", group.id(), "carol", "corr-5");
        var membersAfterRemove = conversationMemberRepository.findByConversationId(group.id());
        var carol = membersAfterRemove.stream().filter(m -> m.getUserId().equals("carol")).findFirst().orElseThrow();
        assertEquals(MemberStatus.REMOVED, carol.getStatus());

        // Départ de "bob"
        service.leave("bob", group.id(), "corr-6");
        var bob = conversationMemberRepository.findByConversationIdAndUserId(group.id(), "bob").orElseThrow();
        assertEquals(MemberStatus.LEFT, bob.getStatus());
    }

    @Test
    void test3_SendMessageWithIdempotencyAndReplay() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-7");
        
        var msg1 = service.send("alice", conv.id(), new SendMessage("client-id-100", MessageType.TEXT, "Hello PostgreSQL", null, null), "corr-8");
        assertNotNull(msg1.id());
        assertEquals("Hello PostgreSQL", msg1.body());

        // Rejeu du même message avec le même clientMessageId
        var replay = service.send("alice", conv.id(), new SendMessage("client-id-100", MessageType.TEXT, "Ignored body", null, null), "corr-9");
        assertEquals(msg1.id(), replay.id());
        assertEquals("Hello PostgreSQL", replay.body());

        // Un seul message persisté
        assertEquals(1, messageRepository.count());
    }

    @Test
    void test4_SendMessageWithAttachmentsAndReply() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-10");
        
        UUID attachment1 = UUID.randomUUID();
        UUID attachment2 = UUID.randomUUID();
        var parent = service.send("alice", conv.id(), new SendMessage("client-parent", MessageType.MEDIA, null, List.of(attachment1, attachment2), null), "corr-11");
        assertEquals(2, parent.attachmentIds().size());

        // Réponse au message parent
        var reply = service.send("bob", conv.id(), new SendMessage("client-reply", MessageType.TEXT, "Superbes photos !", null, parent.id()), "corr-12");
        assertEquals(parent.id(), reply.replyToMessageId());

        // Vérification en base
        var storedParent = messageRepository.findById(parent.id()).orElseThrow();
        assertEquals(2, storedParent.getAttachmentIds().size());
        assertTrue(storedParent.getAttachmentIds().contains(attachment1));
    }

    @Test
    void test5_ListMessagesPaginationAndOrdering() throws InterruptedException {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-13");
        
        for (int i = 1; i <= 5; i++) {
            service.send("alice", conv.id(), new SendMessage("client-batch-" + i, MessageType.TEXT, "Message #" + i, null, null), "corr-" + i);
            Thread.sleep(10); // légère pause pour espacer les sentAt
        }

        // Récupérer les 3 derniers messages (tri DESC)
        MessageSlice slice1 = service.messages("alice", conv.id(), null, 3);
        assertEquals(3, slice1.items().size());
        assertEquals("Message #5", slice1.items().get(0).body());
        assertEquals("Message #4", slice1.items().get(1).body());
        assertEquals("Message #3", slice1.items().get(2).body());
        assertTrue(slice1.hasNext());
        assertNotNull(slice1.nextBefore());

        // Récupérer la suite avec curseur before
        MessageSlice slice2 = service.messages("alice", conv.id(), slice1.nextBefore(), 3);
        assertEquals(2, slice2.items().size());
        assertEquals("Message #2", slice2.items().get(0).body());
        assertEquals("Message #1", slice2.items().get(1).body());
        assertFalse(slice2.hasNext());
    }

    @Test
    void test6_EditMessageWithinWindowAndRejections() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-14");
        var msg = service.send("alice", conv.id(), new SendMessage("client-edit-1", MessageType.TEXT, "Texte initial", null, null), "corr-15");

        // Édition par l'émetteur
        var edited = service.edit("alice", msg.id(), "Texte corrigé", "corr-16");
        assertEquals("Texte corrigé", edited.body());
        assertNotNull(edited.editedAt());

        // Édition par un non-émetteur -> FORBIDDEN
        MessagingException ex = assertThrows(MessagingException.class, () -> service.edit("bob", msg.id(), "Piratage", "corr-17"));
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void test7_DeleteMessageSoftDelete() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-18");
        var msg = service.send("alice", conv.id(), new SendMessage("client-del-1", MessageType.TEXT, "Message à supprimer", null, null), "corr-19");

        service.delete("alice", msg.id(), "corr-20");

        var stored = messageRepository.findById(msg.id()).orElseThrow();
        assertNull(stored.getBody());
        assertTrue(stored.getAttachmentIds().isEmpty());
        assertNotNull(stored.getDeletedAt());
    }

    @Test
    void test8_MarkAsRead() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-21");
        var msg = service.send("alice", conv.id(), new SendMessage("client-read-1", MessageType.TEXT, "Lis-moi !", null, null), "corr-22");

        service.read("bob", conv.id(), msg.id(), "corr-23");

        var memberBob = conversationMemberRepository.findByConversationIdAndUserId(conv.id(), "bob").orElseThrow();
        assertEquals(msg.id(), memberBob.getLastReadMessageId());
        assertNotNull(memberBob.getLastReadAt());
    }

    @Test
    void test9_ListConversationsForUser() {
        var conv1 = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-24");
        var conv2 = service.create("alice", new CreateConversation(ConversationType.GROUP, "Projet YeYamo", Set.of("bob", "carol")), "corr-25");

        List<ConversationSummary> listAlice = service.list("alice");
        assertEquals(2, listAlice.size());

        List<ConversationSummary> listCarol = service.list("carol");
        assertEquals(1, listCarol.size());
        assertEquals("Projet YeYamo", listCarol.get(0).title());
    }

    @Test
    void test10_DirectConversationMembershipCannotChange() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-26");
        MessagingException ex = assertThrows(MessagingException.class, () -> service.addMember("alice", conv.id(), "carol", "corr-27"));
        assertEquals("DIRECT_MEMBERS_IMMUTABLE", ex.getCode());
    }

    @Test
    void test11_NonMemberAccessIsForbidden() {
        var conv = service.create("alice", new CreateConversation(ConversationType.DIRECT, null, Set.of("bob")), "corr-28");
        MessagingException ex = assertThrows(MessagingException.class, () -> service.send("intruder", conv.id(), new SendMessage("client-hack", MessageType.TEXT, "Hacked", null, null), "corr-29"));
        assertEquals("FORBIDDEN", ex.getCode());
    }
}
