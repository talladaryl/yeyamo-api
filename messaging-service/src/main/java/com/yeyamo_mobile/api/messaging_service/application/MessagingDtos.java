package com.yeyamo_mobile.api.messaging_service.application;

import java.time.Instant;
import java.util.*;
import com.yeyamo_mobile.api.messaging_service.domain.*;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.*;

public final class MessagingDtos {
    private MessagingDtos() {}

    public record CreateConversation(ConversationType type, String title, Set<String> participantIds) {}

    public record SendMessage(String clientMessageId, MessageType type, String body, List<UUID> attachmentIds, UUID replyToMessageId) {}

    public record ConversationView(UUID id, ConversationType type, String title, String ownerId, Instant createdAt, Instant updatedAt, UUID lastMessageId, String lastMessagePreview, Instant lastMessageAt, List<MemberView> members) {
        public static ConversationView from(ConversationEntity c, List<ConversationMemberEntity> members) {
            return new ConversationView(c.getId(), c.getType(), c.getTitle(), c.getOwnerId(), c.getCreatedAt(), c.getUpdatedAt(), c.getLastMessageId(), c.getLastMessagePreview(), c.getLastMessageAt(), members.stream().map(MemberView::from).toList());
        }
    }

    /** Persistence projection for the current viewer; member details are batch-enriched by the service. */
    public record InboxConversationRow(UUID id, ConversationType type, String title, MemberRole role, Instant updatedAt, String lastMessagePreview, Instant lastMessageAt) {}

    /** Inbox contract deliberately carries auth-subject member ids for one mobile identity-batch resolution. */
    public record ConversationSummary(UUID id, ConversationType type, String title, MemberRole role, Instant updatedAt, String lastMessagePreview, Instant lastMessageAt, List<String> memberIds, long unreadCount) {}

    public record MemberView(String userId, MemberRole role, MemberStatus status, Instant joinedAt, Instant leftAt, UUID lastReadMessageId, Instant lastReadAt) {
        public static MemberView from(ConversationMemberEntity m) {
            return new MemberView(m.getUserId(), m.getRole(), m.getStatus(), m.getJoinedAt(), m.getLeftAt(), m.getLastReadMessageId(), m.getLastReadAt());
        }
    }

    public record ReplyPreview(UUID id, String senderId, String body, MessageType type, Instant sentAt, boolean deleted) {
        public static ReplyPreview from(MessageEntity message) {
            if (message == null) return null;
            return new ReplyPreview(message.getId(), message.getSenderId(), message.getDeletedAt() == null ? message.getBody() : null, message.getMessageType(), message.getSentAt(), message.getDeletedAt() != null);
        }
    }

    public record MessageView(UUID id, UUID conversationId, String senderId, String clientMessageId, MessageType type, String body, List<UUID> attachmentIds, UUID replyToMessageId, ReplyPreview replyTo, Instant sentAt, Instant editedAt, Instant deletedAt) {
        public static MessageView from(MessageEntity m) {
            return from(m, null);
        }
        public static MessageView from(MessageEntity m, ReplyPreview replyTo) {
            return new MessageView(m.getId(), m.getConversationId(), m.getSenderId(), m.getClientMessageId(), m.getMessageType(), m.getBody(), m.getAttachmentIds(), m.getReplyToMessageId(), replyTo, m.getSentAt(), m.getEditedAt(), m.getDeletedAt());
        }
    }

    public record MessageSlice(List<MessageView> items, Instant nextBefore, UUID nextBeforeId, boolean hasNext) {}
}
