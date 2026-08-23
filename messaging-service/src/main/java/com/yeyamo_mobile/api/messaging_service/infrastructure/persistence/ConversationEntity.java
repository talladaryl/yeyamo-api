package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import com.yeyamo_mobile.api.messaging_service.domain.ConversationType;

@Entity
@Table(name = "conversations")
public class ConversationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ConversationType type;

    @Column(name = "title", length = 120)
    private String title;

    @Column(name = "owner_id", nullable = false, length = 120)
    private String ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    @Column(name = "last_message_preview", columnDefinition = "TEXT")
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public ConversationEntity() {}

    public static ConversationEntity create(ConversationType type, String title, String owner) {
        var c = new ConversationEntity();
        c.id = UUID.randomUUID();
        c.type = type;
        c.title = title;
        c.ownerId = owner;
        c.createdAt = Instant.now();
        c.updatedAt = c.createdAt;
        return c;
    }

    public void message(UUID id, String preview, Instant at) {
        this.lastMessageId = id;
        this.lastMessagePreview = preview;
        this.lastMessageAt = at;
        this.updatedAt = at;
    }

    public void rename(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ConversationType getType() { return type; }
    public String getTitle() { return title; }
    public String getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getLastMessageId() { return lastMessageId; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public Instant getLastMessageAt() { return lastMessageAt; }
}
