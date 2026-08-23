package com.yeyamo_mobile.api.messaging_service.application;

import static com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.messaging_service.application.port.MessagingEventPort;
import com.yeyamo_mobile.api.messaging_service.domain.*;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.*;

@Service
@Transactional
public class MessagingApplicationService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final DirectConversationRepository direct;
    private final MessageRepository messages;
    private final MessageIdempotencyRepository idempotency;
    private final MessagingEventPort events;
    private final int groupMax;
    private final int messageMax;
    private final int attachmentMax;
    private final int editMinutes;

    public MessagingApplicationService(
            ConversationRepository conversations,
            ConversationMemberRepository members,
            DirectConversationRepository direct,
            MessageRepository messages,
            MessageIdempotencyRepository idempotency,
            MessagingEventPort events,
            @Value("${messaging.group.max-members:100}") int groupMax,
            @Value("${messaging.message.max-length:4000}") int messageMax,
            @Value("${messaging.message.max-attachments:10}") int attachmentMax,
            @Value("${messaging.message.edit-window-minutes:15}") int editMinutes) {
        this.conversations = conversations;
        this.members = members;
        this.direct = direct;
        this.messages = messages;
        this.idempotency = idempotency;
        this.events = events;
        this.groupMax = groupMax;
        this.messageMax = messageMax;
        this.attachmentMax = attachmentMax;
        this.editMinutes = editMinutes;
    }

    public ConversationView create(String actor, CreateConversation command, String correlation) {
        ConversationType type = Objects.requireNonNull(command.type(), "type");
        Set<String> participants = new LinkedHashSet<>(command.participantIds() == null ? Set.of() : command.participantIds());
        participants.remove(actor);
        if (type == ConversationType.DIRECT) {
            if (participants.size() != 1) {
                throw error("INVALID_DIRECT_MEMBERS", "Direct conversation requires exactly one other participant");
            }
            String pair = pair(actor, participants.iterator().next());
            var existing = direct.findById(pair);
            if (existing.isPresent()) {
                return get(actor, existing.get().getConversationId());
            }
        } else {
            if (command.title() == null || command.title().isBlank()) {
                throw error("GROUP_TITLE_REQUIRED", "Group title is required");
            }
            if (participants.size() + 1 > groupMax) {
                throw error("TOO_MANY_MEMBERS", "Group member limit exceeded");
            }
        }
        ConversationEntity conversation = conversations.save(ConversationEntity.create(type, normalizeTitle(type, command.title()), actor));
        List<ConversationMemberEntity> saved = new ArrayList<>();
        saved.add(members.save(ConversationMemberEntity.active(conversation.getId(), actor, MemberRole.OWNER)));
        for (String user : participants) {
            saved.add(members.save(ConversationMemberEntity.active(conversation.getId(), required(user, "participantId"), MemberRole.MEMBER)));
        }
        if (type == ConversationType.DIRECT) {
            direct.save(new DirectConversationEntity(pair(actor, participants.iterator().next()), conversation.getId()));
        }
        events.publish("messaging.conversation.created", conversation.getId().toString(), actor, correlation, participants, Map.of("conversationId", conversation.getId(), "conversationType", type.name(), "actorId", actor), ConversationView.from(conversation, saved));
        return ConversationView.from(conversation, saved);
    }

    @Transactional(readOnly = true)
    public ConversationView get(String actor, UUID id) {
        ConversationEntity c = conversation(id);
        activeMember(id, actor);
        return ConversationView.from(c, members.findByConversationId(id));
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> list(String actor) {
        return members.findUserConversationSummaries(actor, MemberStatus.ACTIVE);
    }

    public ConversationView addMember(String actor, UUID id, String user, String correlation) {
        ConversationEntity c = conversation(id);
        ensureGroup(c);
        ConversationMemberEntity admin = activeMember(id, actor);
        if (admin.getRole() != MemberRole.OWNER && admin.getRole() != MemberRole.ADMIN) {
            throw error("FORBIDDEN", "Administrator role required");
        }
        List<ConversationMemberEntity> current = members.findByConversationId(id);
        if (current.stream().filter(x -> x.getStatus() == MemberStatus.ACTIVE).count() >= groupMax) {
            throw error("TOO_MANY_MEMBERS", "Group member limit exceeded");
        }
        var existing = members.findByConversationIdAndUserId(id, user);
        if (existing.isPresent() && existing.get().getStatus() == MemberStatus.ACTIVE) {
            return ConversationView.from(c, current);
        }
        ConversationMemberEntity added;
        if (existing.isPresent()) {
            added = existing.get();
            added = members.save(ConversationMemberEntity.active(id, user, MemberRole.MEMBER));
        } else {
            added = members.save(ConversationMemberEntity.active(id, user, MemberRole.MEMBER));
        }
        events.publish("messaging.member.added", id.toString(), actor, correlation, List.of(user), Map.of("conversationId", id, "userId", user, "actorId", actor), MemberView.from(added));
        return ConversationView.from(c, members.findByConversationId(id));
    }

    public void removeMember(String actor, UUID id, String user, String correlation) {
        ConversationEntity c = conversation(id);
        ensureGroup(c);
        ConversationMemberEntity admin = activeMember(id, actor);
        if (admin.getRole() != MemberRole.OWNER && admin.getRole() != MemberRole.ADMIN) {
            throw error("FORBIDDEN", "Administrator role required");
        }
        ConversationMemberEntity target = activeMember(id, user);
        if (target.getRole() == MemberRole.OWNER) {
            throw error("OWNER_CANNOT_BE_REMOVED", "Conversation owner cannot be removed");
        }
        target.leave(MemberStatus.REMOVED);
        members.save(target);
        events.publish("messaging.member.removed", id.toString(), actor, correlation, List.of(user), Map.of("conversationId", id, "userId", user, "actorId", actor), Map.of("conversationId", id, "userId", user));
    }

    public void leave(String actor, UUID id, String correlation) {
        ConversationEntity c = conversation(id);
        ConversationMemberEntity member = activeMember(id, actor);
        if (member.getRole() == MemberRole.OWNER && c.getType() == ConversationType.GROUP) {
            throw error("OWNER_CANNOT_LEAVE", "Transfer ownership before leaving");
        }
        member.leave(MemberStatus.LEFT);
        members.save(member);
        events.publish("messaging.member.left", id.toString(), actor, correlation, recipientIds(id, actor), Map.of("conversationId", id, "userId", actor), Map.of("conversationId", id, "userId", actor));
    }

    public MessageView send(String actor, UUID id, SendMessage command, String correlation) {
        activeMember(id, actor);
        String client = required(command.clientMessageId(), "clientMessageId");
        var replay = idempotency.findBySenderIdAndClientMessageId(actor, client);
        if (replay.isPresent()) {
            MessageEntity existing = messages.findById(replay.get().getMessageId()).orElseThrow(() -> error("MESSAGE_NOT_FOUND", "Message replay target not found"));
            MessageView view = MessageView.from(existing);
            events.publish("messaging.message.sent", id.toString(), actor, correlation, recipientIds(id, actor), messagePayload(view), view);
            return view;
        }
        validate(command);
        if (command.replyToMessageId() != null) {
            MessageEntity parent = message(command.replyToMessageId());
            if (!parent.getConversationId().equals(id)) {
                throw error("INVALID_REPLY", "Reply target belongs to another conversation");
            }
        }
        MessageEntity saved = messages.save(MessageEntity.create(id, actor, client, command.type(), normalizeBody(command.body()), command.attachmentIds(), command.replyToMessageId()));
        idempotency.save(new MessageIdempotencyEntity(actor, client, saved.getId(), id));
        ConversationEntity conversation = conversation(id);
        conversation.message(saved.getId(), preview(saved), saved.getSentAt());
        conversations.save(conversation);
        MessageView view = MessageView.from(saved);
        events.publish("messaging.message.sent", id.toString(), actor, correlation, recipientIds(id, actor), messagePayload(view), view);
        return view;
    }

    @Transactional(readOnly = true)
    public MessageSlice messages(String actor, UUID id, Instant before, int limit) {
        activeMember(id, actor);
        int size = Math.max(1, Math.min(limit, 100));
        var slice = before == null 
                ? messages.findByConversationIdOrderBySentAtDesc(id, PageRequest.of(0, size)) 
                : messages.findByConversationIdAndSentAtLessThanOrderBySentAtDesc(id, before, PageRequest.of(0, size));
        List<MessageView> items = slice.getContent().stream().map(MessageView::from).toList();
        Instant next = items.isEmpty() ? null : items.get(items.size() - 1).sentAt();
        return new MessageSlice(items, next, slice.hasNext());
    }

    public MessageView edit(String actor, UUID messageId, String body, String correlation) {
        MessageEntity stored = message(messageId);
        activeMember(stored.getConversationId(), actor);
        if (!stored.getSenderId().equals(actor)) {
            throw error("FORBIDDEN", "Only sender can edit message");
        }
        if (stored.getDeletedAt() != null) {
            throw error("MESSAGE_DELETED", "Deleted message cannot be edited");
        }
        if (stored.getSentAt().plus(Duration.ofMinutes(editMinutes)).isBefore(Instant.now())) {
            throw error("EDIT_WINDOW_EXPIRED", "Message edit window expired");
        }
        String normalized = normalizeBody(body);
        if (normalized == null) {
            throw error("EMPTY_MESSAGE", "Message body is required");
        }
        stored.edit(normalized);
        messages.save(stored);
        MessageView view = MessageView.from(stored);
        events.publish("messaging.message.edited", stored.getConversationId().toString(), actor, correlation, recipientIds(stored.getConversationId(), actor), messagePayload(view), view);
        return view;
    }

    public void delete(String actor, UUID messageId, String correlation) {
        MessageEntity stored = message(messageId);
        activeMember(stored.getConversationId(), actor);
        if (!stored.getSenderId().equals(actor)) {
            throw error("FORBIDDEN", "Only sender can delete message");
        }
        if (stored.getDeletedAt() != null) {
            return;
        }
        stored.delete();
        messages.save(stored);
        events.publish("messaging.message.deleted", stored.getConversationId().toString(), actor, correlation, recipientIds(stored.getConversationId(), actor), Map.of("conversationId", stored.getConversationId(), "messageId", messageId, "senderId", actor), MessageView.from(stored));
    }

    public void read(String actor, UUID id, UUID messageId, String correlation) {
        ConversationMemberEntity member = activeMember(id, actor);
        MessageEntity message = message(messageId);
        if (!message.getConversationId().equals(id)) {
            throw error("INVALID_MESSAGE", "Message belongs to another conversation");
        }
        member.read(messageId, Instant.now());
        members.save(member);
        events.publish("messaging.message.read", id.toString(), actor, correlation, recipientIds(id, actor), Map.of("conversationId", id, "messageId", messageId, "readerId", actor), Map.of("conversationId", id, "messageId", messageId, "readerId", actor));
    }

    @Transactional(readOnly = true)
    public boolean isActiveMember(String user, UUID conversation) {
        return members.findByConversationIdAndUserId(conversation, user).filter(m -> m.getStatus() == MemberStatus.ACTIVE).isPresent();
    }

    private ConversationEntity conversation(UUID id) {
        return conversations.findById(id).orElseThrow(() -> error("CONVERSATION_NOT_FOUND", "Conversation not found"));
    }

    private MessageEntity message(UUID id) {
        return messages.findById(id).orElseThrow(() -> error("MESSAGE_NOT_FOUND", "Message not found"));
    }

    private ConversationMemberEntity activeMember(UUID id, String user) {
        return members.findByConversationIdAndUserId(id, user).filter(m -> m.getStatus() == MemberStatus.ACTIVE).orElseThrow(() -> error("FORBIDDEN", "User is not an active conversation member"));
    }

    private void ensureGroup(ConversationEntity c) {
        if (c.getType() != ConversationType.GROUP) {
            throw error("DIRECT_MEMBERS_IMMUTABLE", "Direct conversation membership cannot change");
        }
    }

    private List<String> recipientIds(UUID id, String excluded) {
        return members.findByConversationId(id).stream().filter(m -> m.getStatus() == MemberStatus.ACTIVE && !m.getUserId().equals(excluded)).map(ConversationMemberEntity::getUserId).toList();
    }

    private void validate(SendMessage c) {
        if (c.type() == null || c.type() == MessageType.SYSTEM) {
            throw error("INVALID_MESSAGE_TYPE", "Client message type is invalid");
        }
        String body = normalizeBody(c.body());
        List<UUID> a = c.attachmentIds() == null ? List.of() : c.attachmentIds();
        if (a.size() > attachmentMax) {
            throw error("TOO_MANY_ATTACHMENTS", "Attachment limit exceeded");
        }
        if (body != null && body.length() > messageMax) {
            throw error("MESSAGE_TOO_LONG", "Message length exceeded");
        }
        if (c.type() == MessageType.TEXT && (body == null || !a.isEmpty()) || c.type() == MessageType.MEDIA && a.isEmpty() || c.type() == MessageType.MIXED && (body == null || a.isEmpty())) {
            throw error("INVALID_MESSAGE_CONTENT", "Message content does not match its type");
        }
    }

    private Map<String, Object> messagePayload(MessageView m) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("conversationId", m.conversationId());
        p.put("messageId", m.id());
        p.put("senderId", m.senderId());
        p.put("messageType", m.type());
        p.put("bodyPreview", m.body() == null ? "" : m.body().substring(0, Math.min(120, m.body().length())));
        p.put("attachmentIds", m.attachmentIds());
        p.put("sentAt", m.sentAt());
        return p;
    }

    private String preview(MessageEntity m) {
        if (m.getBody() != null) {
            return m.getBody().substring(0, Math.min(120, m.getBody().length()));
        }
        return "[Media]";
    }

    private String pair(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "#" + b : b + "#" + a;
    }

    private String normalizeTitle(ConversationType t, String v) {
        if (t == ConversationType.DIRECT) {
            return null;
        }
        String x = required(v, "title").trim();
        return x.length() > 120 ? x.substring(0, 120) : x;
    }

    private String normalizeBody(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private String required(String v, String field) {
        if (v == null || v.isBlank()) {
            throw error("INVALID_REQUEST", field + " is required");
        }
        return v;
    }

    private MessagingException error(String c, String m) {
        return new MessagingException(c, m);
    }
}
