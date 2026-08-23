package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import com.yeyamo_mobile.api.messaging_service.domain.MessageType;

@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false, length = 120)
    private String senderId;

    @Column(name = "client_message_id", nullable = false, length = 120)
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_attachments", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "attachment_id", nullable = false)
    @OrderColumn(name = "position")
    private List<UUID> attachmentIds = new ArrayList<>();

    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public MessageEntity() {}

    public static MessageEntity create(UUID conversation, String sender, String client, MessageType type, String body, List<UUID> attachments, UUID reply) {
        var m = new MessageEntity();
        m.id = UUID.randomUUID();
        m.conversationId = conversation;
        m.senderId = sender;
        m.clientMessageId = client;
        m.messageType = type;
        m.body = body;
        m.attachmentIds = attachments == null ? new ArrayList<>() : new ArrayList<>(attachments);
        m.replyToMessageId = reply;
        m.sentAt = Instant.now();
        return m;
    }

    public void edit(String body) {
        this.body = body;
        this.editedAt = Instant.now();
    }

    public void delete() {
        this.body = null;
        if (this.attachmentIds != null) {
            this.attachmentIds.clear();
        }
        this.deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getMessageId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public String getSenderId() { return senderId; }
    public String getClientMessageId() { return clientMessageId; }
    public MessageType getMessageType() { return messageType; }
    public String getBody() { return body; }
    public List<UUID> getAttachmentIds() { return attachmentIds == null ? List.of() : Collections.unmodifiableList(attachmentIds); }
    public UUID getReplyToMessageId() { return replyToMessageId; }
    public Instant getSentAt() { return sentAt; }
    public Instant getEditedAt() { return editedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
