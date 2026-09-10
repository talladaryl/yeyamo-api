package com.yeyamo_mobile.api.feed_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.feed_service.application.port.FeedCachePort;
import com.yeyamo_mobile.api.feed_service.application.port.FeedProjectionPort;
import com.yeyamo_mobile.api.feed_service.application.ranking.RankingStrategy;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedCandidate;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedMetric;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedPost;

class PublicFeedQueryServiceTests {
    @Test
    void returnsOnlyPublicPublishedPostsInStableOrder() {
        Instant older = Instant.parse("2026-01-01T00:00:00Z");
        Instant newer = Instant.parse("2026-01-02T00:00:00Z");
        FeedPost visible = post(UUID.randomUUID(), "PUBLIC", "PUBLISHED", newer);
        FeedPost privatePost = post(UUID.randomUUID(), "PRIVATE", "PUBLISHED", older);
        FeedPost draft = post(UUID.randomUUID(), "PUBLIC", "DRAFT", older);
        FeedQueryService service = new FeedQueryService(new Projection(List.of(privatePost, draft, visible)), new Cache(),
                (candidate, affinity, now) -> 0d, new AdInjectionService(null, false, 3, 8), new FeedMixProperties());

        PublicFeedPage page = service.publicFeed(0, 20);

        assertEquals(1, page.items().size());
        assertEquals(visible.postId(), page.items().get(0).postId());
        assertEquals("author", page.items().get(0).authorId());
    }

    private static FeedPost post(UUID id, String visibility, String status, Instant publishedAt) {
        return new FeedPost(id, "author", "caption", visibility, status, null, "NONE", null, List.of(), List.of(), null, null, publishedAt, publishedAt);
    }

    private static final class Projection implements FeedProjectionPort {
        private final List<FeedPost> posts;
        private Projection(List<FeedPost> posts) { this.posts = posts; }
        @Override public List<FeedCandidate> candidates(int limit) { return posts.stream().map(post -> new FeedCandidate(post, FeedMetric.empty(post.postId()))).toList(); }
        @Override public void savePost(FeedPost post) { }
        @Override public Optional<FeedPost> findPost(UUID id) { return Optional.empty(); }
        @Override public FeedMetric metric(UUID id) { return FeedMetric.empty(id); }
        @Override public void saveMetric(FeedMetric metric) { }
        @Override public void adjustSignal(String user, UUID post, double weight) { }
        @Override public Map<String, Double> authorAffinities(String user) { return Map.of(); }
    }

    private static final class Cache implements FeedCachePort {
        @Override public Optional<FeedPage> get(String user, int page, int size) { return Optional.empty(); }
        @Override public void put(FeedPage page) { }
        @Override public void invalidate() { }
    }
}
