package com.yeyamo_mobile.api.notification_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InteractionNotificationPolicyTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ContentOwnerClient owners = mock(ContentOwnerClient.class);
    private final EventNotificationPolicy policy = new EventNotificationPolicy(mapper, owners);

    @Test void likeTargetsCanonicalPostOwnerAndKeepsStructuredPostId() throws Exception {
        UUID post = UUID.randomUUID(); when(owners.postOwner(post)).thenReturn("owner-b");
        var intents = policy.map(mapper.readTree(event("interaction.like.added", "{\"postId\":\""+post+"\",\"userId\":\"actor-a\"}")));
        assertEquals(1, intents.size()); assertEquals("owner-b", intents.getFirst().recipientId());
        assertTrue(intents.getFirst().dataJson().contains(post.toString()));
    }
    @Test void selfLikeIsSuppressed() throws Exception {
        UUID post = UUID.randomUUID(); when(owners.postOwner(post)).thenReturn("owner-b");
        assertTrue(policy.map(mapper.readTree(event("interaction.like.added", "{\"postId\":\""+post+"\",\"userId\":\"owner-b\"}"))).isEmpty());
    }
    @Test void replyTargetsParentAuthorAndSelfReplyIsSuppressed() throws Exception {
        UUID post=UUID.randomUUID(), comment=UUID.randomUUID(), parent=UUID.randomUUID();
        String payload="{\"postId\":\""+post+"\",\"commentId\":\""+comment+"\",\"parentCommentId\":\""+parent+"\",\"authorId\":\"actor-a\",\"parentAuthorId\":\"author-b\"}";
        var intents=policy.map(mapper.readTree(event("interaction.comment.created",payload)));
        assertEquals("author-b",intents.getFirst().recipientId());assertTrue(intents.getFirst().dataJson().contains(parent.toString()));
        assertTrue(policy.map(mapper.readTree(event("interaction.comment.created",payload.replace("actor-a","author-b")))).isEmpty());
    }
    @Test void persistedShareTargetsPostOwner() throws Exception {
        UUID post=UUID.randomUUID();when(owners.postOwner(post)).thenReturn("owner-b");
        var intents=policy.map(mapper.readTree(event("interaction.post.shared","{\"postId\":\""+post+"\",\"userId\":\"actor-a\",\"shareId\":\""+UUID.randomUUID()+"\"}")));
        assertEquals("owner-b",intents.getFirst().recipientId());
    }
    private String event(String type,String payload){return "{\"eventId\":\""+UUID.randomUUID()+"\",\"eventType\":\""+type+"\",\"eventVersion\":1,\"payload\":"+payload+"}";}
}
