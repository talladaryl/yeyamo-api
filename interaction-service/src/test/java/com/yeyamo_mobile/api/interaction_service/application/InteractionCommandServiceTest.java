package com.yeyamo_mobile.api.interaction_service.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.interaction_service.application.port.*;
import com.yeyamo_mobile.api.interaction_service.domain.model.*;
import com.yeyamo_mobile.api.interaction_service.domain.port.*;

class InteractionCommandServiceTest {
    private final MemoryRelations relations = new MemoryRelations();
    private final MemoryComments comments = new MemoryComments();
    private final MemoryShares shares = new MemoryShares();
    private final MemoryCheckIns checkIns = new MemoryCheckIns();
    private final MemoryReceipts receipts = new MemoryReceipts();
    private final RecordingOutbox outbox = new RecordingOutbox();
    private final RecordingCache cache = new RecordingCache();
    private InteractionCommandService service;

    @BeforeEach
    void setUp() {
        service = new InteractionCommandService(relations, comments, shares, checkIns, receipts, outbox, cache);
    }

    @Test
    void sameLikeCommandIsReplayedWithoutDuplicateOrSecondEvent() {
        UUID postId = UUID.randomUUID();

        CommandResult first = service.addRelation(postId, "user-1", RelationType.LIKE, "like-key", "corr-1");
        CommandResult replay = service.addRelation(postId, "user-1", RelationType.LIKE, "like-key", "corr-1");

        assertTrue(first.changed());
        assertFalse(first.replayed());
        assertTrue(replay.replayed());
        assertEquals(first.resourceId(), replay.resourceId());
        assertEquals(1, relations.values.size());
        assertEquals(List.of("interaction.like.added"), outbox.events);
        assertEquals(List.of(postId), cache.evictions);
    }

    @Test
    void relationCommandsAreSemanticallyIdempotentWithDifferentKeys() {
        UUID postId = UUID.randomUUID();
        service.addRelation(postId, "user-1", RelationType.FAVORITE, "key-1", null);

        CommandResult alreadyPresent = service.addRelation(postId, "user-1", RelationType.FAVORITE, "key-2", null);
        CommandResult removed = service.removeRelation(postId, "user-1", RelationType.FAVORITE, "key-3", null);
        CommandResult alreadyRemoved = service.removeRelation(postId, "user-1", RelationType.FAVORITE, "key-4", null);

        assertFalse(alreadyPresent.changed());
        assertTrue(removed.changed());
        assertFalse(alreadyRemoved.changed());
        assertTrue(relations.values.isEmpty());
        assertEquals(List.of("interaction.favorite.added", "interaction.favorite.removed"), outbox.events);
    }

    @Test
    void commentCanOnlyBeChangedByItsAuthorOrModeratorAndDeletionIsSoft() {
        UUID postId = UUID.randomUUID();
        Comment created = service.addComment(postId, null, "author", "Hello", "comment-1", null);

        InteractionException forbidden = assertThrows(InteractionException.class,
                () -> service.updateComment(created.getId(), "intruder", false, "No", "comment-2", null));
        assertEquals("INTERACTION_FORBIDDEN", forbidden.getCode());

        service.updateComment(created.getId(), "moderator", true, "Moderated", "comment-3", null);
        service.deleteComment(created.getId(), "author", false, "comment-4", null);
        service.deleteComment(created.getId(), "author", false, "comment-4", null);

        assertEquals(CommentStatus.DELETED, comments.values.get(created.getId()).getStatus());
        assertEquals(3, outbox.events.size());
    }

    @Test
    void shareAndCheckInRetriesReturnTheOriginalResource() {
        UUID postId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PostShare share = service.share(postId, "user-1", "email", "share-key", null);
        PostShare replayedShare = service.share(postId, "user-1", "email", "share-key", null);
        CheckIn checkIn = service.checkIn(assetId, "user-1", 48.85, 2.35, true, "check-key", null);
        CheckIn replayedCheckIn = service.checkIn(assetId, "user-1", 48.85, 2.35, true, "check-key", null);

        assertEquals(share.id(), replayedShare.id());
        assertEquals(checkIn.id(), replayedCheckIn.id());
        assertEquals(1, shares.values.size());
        assertEquals(1, checkIns.values.size());
        assertEquals(List.of("interaction.post.shared", "interaction.checkin.created"), outbox.events);
    }

    private static final class MemoryRelations implements RelationRepository {
        final List<PostRelation> values = new ArrayList<>();
        public Optional<PostRelation> find(UUID post, String user, RelationType type) { return values.stream().filter(v -> v.postId().equals(post) && v.userId().equals(user) && v.type() == type).findFirst(); }
        public PostRelation save(PostRelation value) { values.add(value); return value; }
        public void delete(PostRelation value) { values.remove(value); }
        public long count(UUID post, RelationType type) { return values.stream().filter(v -> v.postId().equals(post) && v.type() == type).count(); }
        public List<PostRelation> findFavorites(String user, int limit) { return values.stream().filter(v -> v.userId().equals(user) && v.type() == RelationType.FAVORITE).limit(limit).toList(); }
    }

    private static final class MemoryComments implements CommentRepository {
        final Map<UUID, Comment> values = new HashMap<>();
        public Comment save(Comment value) { values.put(value.getId(), value); return value; }
        public Optional<Comment> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public List<Comment> findActiveByPost(UUID post, int limit) { return values.values().stream().filter(v -> v.getPostId().equals(post) && v.getStatus() == CommentStatus.ACTIVE).limit(limit).toList(); }
        public long countActiveByPost(UUID post) { return findActiveByPost(post, Integer.MAX_VALUE).size(); }
    }

    private static final class MemoryShares implements ShareRepository {
        final Map<UUID, PostShare> values = new HashMap<>();
        public PostShare save(PostShare value) { values.put(value.id(), value); return value; }
        public Optional<PostShare> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public long countByPost(UUID post) { return values.values().stream().filter(v -> v.postId().equals(post)).count(); }
    }

    private static final class MemoryCheckIns implements CheckInRepository {
        final Map<UUID, CheckIn> values = new HashMap<>();
        public CheckIn save(CheckIn value) { values.put(value.id(), value); return value; }
        public Optional<CheckIn> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public List<CheckIn> findByUser(String user, int limit) { return values.values().stream().filter(v -> v.userId().equals(user)).limit(limit).toList(); }
    }

    private static final class MemoryReceipts implements CommandReceiptPort {
        final List<CommandReceipt> values = new ArrayList<>();
        public Optional<CommandReceipt> find(String key, String actor, String operation) { return values.stream().filter(v -> v.idempotencyKey().equals(key) && v.actorId().equals(actor) && v.operation().equals(operation)).findFirst(); }
        public CommandReceipt save(CommandReceipt value) { values.add(value); return value; }
    }

    private static final class RecordingOutbox implements InteractionOutboxPort {
        final List<String> events = new ArrayList<>();
        public void append(String type, String aggregate, String id, String actor, String correlation, Map<String, Object> payload) { events.add(type); }
    }

    private static final class RecordingCache implements InteractionCachePort {
        final List<UUID> evictions = new ArrayList<>();
        public Optional<InteractionSummary.Counts> getCounts(UUID postId) { return Optional.empty(); }
        public void putCounts(UUID postId, InteractionSummary.Counts counts) { }
        public void evict(UUID postId) { evictions.add(postId); }
    }
}
