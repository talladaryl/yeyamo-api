package com.yeyamo_mobile.api.messaging_service.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

public class MessageIdempotencyId implements Serializable {
    private String senderId;
    private String clientMessageId;

    public MessageIdempotencyId() {}

    public MessageIdempotencyId(String senderId, String clientMessageId) {
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageIdempotencyId that = (MessageIdempotencyId) o;
        return Objects.equals(senderId, that.senderId) && Objects.equals(clientMessageId, that.clientMessageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, clientMessageId);
    }
}
