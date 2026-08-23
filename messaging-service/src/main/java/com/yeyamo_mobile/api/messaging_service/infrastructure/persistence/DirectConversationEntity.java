package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "direct_conversations")
public class DirectConversationEntity {
    @Id
    @Column(name = "participant_pair", nullable = false, length = 255)
    private String participantPair;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DirectConversationEntity() {}

    public DirectConversationEntity(String pair, UUID conversation) {
        this.participantPair = pair;
        this.conversationId = conversation;
        this.createdAt = Instant.now();
    }

    public String getParticipantPair() { return participantPair; }
    public UUID getConversationId() { return conversationId; }
    public Instant getCreatedAt() { return createdAt; }
}
