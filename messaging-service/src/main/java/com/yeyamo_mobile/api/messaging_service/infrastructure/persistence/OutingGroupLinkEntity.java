package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The durable source-of-truth association between a public outing and its group chat. */
@Entity
@Table(name = "outing_group_links")
public class OutingGroupLinkEntity {
    @Id @Column(name = "outing_id") private UUID outingId;
    @Column(name = "conversation_id", nullable = false, unique = true) private UUID conversationId;
    @Column(name = "owner_user_id", nullable = false, length = 120) private String ownerUserId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static OutingGroupLinkEntity create(UUID outingId, UUID conversationId, String ownerUserId) {
        OutingGroupLinkEntity link = new OutingGroupLinkEntity();
        link.outingId = outingId;
        link.conversationId = conversationId;
        link.ownerUserId = ownerUserId;
        link.createdAt = Instant.now();
        return link;
    }
    public UUID getOutingId() { return outingId; }
    public UUID getConversationId() { return conversationId; }
    public String getOwnerUserId() { return ownerUserId; }
}
