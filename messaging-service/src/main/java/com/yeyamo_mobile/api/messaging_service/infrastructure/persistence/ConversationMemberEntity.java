package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import com.yeyamo_mobile.api.messaging_service.domain.*;

@Entity
@Table(name = "conversation_members")
@IdClass(ConversationMemberId.class)
public class ConversationMemberEntity {
    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Id
    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    public ConversationMemberEntity() {}

    public static ConversationMemberEntity active(UUID conversation, String user, MemberRole role) {
        var m = new ConversationMemberEntity();
        m.conversationId = conversation;
        m.userId = user;
        m.role = role;
        m.status = MemberStatus.ACTIVE;
        m.joinedAt = Instant.now();
        return m;
    }

    public void leave(MemberStatus status) {
        this.status = status;
        this.leftAt = Instant.now();
    }

    public void read(UUID message, Instant at) {
        this.lastReadMessageId = message;
        this.lastReadAt = at;
    }

    public void promote(MemberRole role) {
        this.role = role;
    }

    public UUID getConversationId() { return conversationId; }
    public String getUserId() { return userId; }
    public MemberRole getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public UUID getLastReadMessageId() { return lastReadMessageId; }
    public Instant getLastReadAt() { return lastReadAt; }
}
