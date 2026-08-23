package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ConversationMemberId implements Serializable {
    private UUID conversationId;
    private String userId;

    public ConversationMemberId() {}

    public ConversationMemberId(UUID conversationId, String userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationMemberId that = (ConversationMemberId) o;
        return Objects.equals(conversationId, that.conversationId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}
