package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "message_idempotency")
@IdClass(MessageIdempotencyId.class)
public class MessageIdempotencyEntity {
    @Id
    @Column(name = "sender_id", nullable = false, length = 120)
    private String senderId;

    @Id
    @Column(name = "client_message_id", nullable = false, length = 120)
    private String clientMessageId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MessageIdempotencyEntity() {}

    public MessageIdempotencyEntity(String sender, String client, UUID message, UUID conversation) {
        this.senderId = sender;
        this.clientMessageId = client;
        this.messageId = message;
        this.conversationId = conversation;
        this.createdAt = Instant.now();
    }

    public String getSenderId() { return senderId; }
    public String getClientMessageId() { return clientMessageId; }
    public UUID getMessageId() { return messageId; }
    public UUID getConversationId() { return conversationId; }
    public Instant getCreatedAt() { return createdAt; }
}
