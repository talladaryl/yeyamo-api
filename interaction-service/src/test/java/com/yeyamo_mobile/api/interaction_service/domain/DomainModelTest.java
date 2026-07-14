package com.yeyamo_mobile.api.interaction_service.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.interaction_service.domain.model.CheckIn;
import com.yeyamo_mobile.api.interaction_service.domain.model.Comment;
import com.yeyamo_mobile.api.interaction_service.domain.model.CommentStatus;
import com.yeyamo_mobile.api.interaction_service.domain.model.PostShare;
import com.yeyamo_mobile.api.interaction_service.domain.model.PostRelation;

class DomainModelTest {

    @Test
    void commentIsNormalizedUpdatedAndSoftDeleted() {
        Comment comment = Comment.create(UUID.randomUUID(), null, "user-1", "  First message  ");

        assertEquals("First message", comment.getBody());
        comment.update("Edited");
        assertEquals("Edited", comment.getBody());

        comment.delete();
        assertEquals(CommentStatus.DELETED, comment.getStatus());
        assertNull(comment.getBody());
        assertNotNull(comment.getDeletedAt());
        assertThrows(IllegalStateException.class, () -> comment.update("Forbidden"));
    }

    @Test
    void rejectsInvalidContentAndCoordinates() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> Comment.create(id, null, "user-1", " "));
        assertThrows(IllegalArgumentException.class, () -> CheckIn.create(id, "user-1", 91d, 2d, true));
        assertThrows(IllegalArgumentException.class, () -> CheckIn.create(id, "user-1", 2d, -181d, true));
        assertThrows(IllegalArgumentException.class, () -> PostShare.create(id, " ", "email"));
        assertThrows(IllegalArgumentException.class, () -> PostRelation.create(id, "user-1", null));
    }

    @Test
    void shareChannelIsNormalizedAndDefaultsToInternal() {
        UUID postId = UUID.randomUUID();
        assertEquals("WHATSAPP", PostShare.create(postId, "user-1", " whatsapp ").channel());
        assertEquals("INTERNAL", PostShare.create(postId, "user-1", null).channel());
    }
}
