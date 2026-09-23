package com.yeyamo_mobile.api.feed_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.feed_service.application.port.FeedCachePort;
import com.yeyamo_mobile.api.feed_service.application.port.FeedProjectionPort;
import com.yeyamo_mobile.api.feed_service.application.ranking.PersonalizedRankingStrategy;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedCandidate;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedMetric;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedPost;

class FeedQueryServiceTests {

    @Test
    void cacheMissAndCacheHitBothInjectSponsoredItemsWithoutCachingThem() {
        List<FeedCandidate> candidates = candidates(8);
        MemoryCache cache = new MemoryCache();
        AdInjectionService ads = mock(AdInjectionService.class);
        FeedItem sponsored = FeedItem.sponsored("delivery-1", "campaign-1", "EVENT", "event-1", Map.of(), BigDecimal.ONE, "token-1");
        when(ads.injectAds(anyList(), anyString(), anyInt(), anyString())).thenAnswer(invocation -> {
            List<FeedItem> result = new ArrayList<>(invocation.getArgument(0));
            result.add(sponsored);
            return result;
        });

        FeedQueryService service = service(candidates, cache, ads);
        FeedPage first = service.feed("u1", 0, 8, "corr-1");
        FeedPage second = service.feed("u1", 0, 8, "corr-2");

        verify(ads, times(2)).injectAds(anyList(), anyString(), anyInt(), anyString());
        assertTrue(first.items().stream().anyMatch(FeedItem::isSponsored));
        assertTrue(second.items().stream().anyMatch(FeedItem::isSponsored));
        assertTrue(cache.value.items().stream().allMatch(FeedItem::isOrganic));
        assertNotNull(cache.value);
    }

    @Test
    void hasNextComesFromOrganicPaginationBeforeSponsoredInjection() {
        AdInjectionService ads = mock(AdInjectionService.class);
        when(ads.injectAds(anyList(), anyString(), anyInt(), anyString())).thenAnswer(invocation -> {
            List<FeedItem> result = new ArrayList<>(invocation.getArgument(0));
            result.add(FeedItem.sponsored("delivery-1", "campaign-1", "EVENT", "event-1", Map.of(), BigDecimal.ONE, "token-1"));
            return result;
        });

        FeedQueryService service = service(candidates(3), new MemoryCache(), ads);
        FeedPage first = service.feed("u1", 0, 2, "corr-1");
        FeedPage last = service.feed("u1", 1, 2, "corr-2");

        assertTrue(first.hasNext());
        assertEquals(3, first.items().size(), "two organic items plus one sponsored item");
        assertFalse(last.hasNext());
        assertEquals(2, last.items().size(), "one organic item plus one sponsored item");
    }

    @Test
    void hasNextHandlesEmptyAndShortOrganicPages() {
        AdInjectionService ads = mock(AdInjectionService.class);
        when(ads.injectAds(anyList(), anyString(), anyInt(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        FeedPage empty = service(List.of(), new MemoryCache(), ads).feed("u1", 0, 2, "corr-empty");
        FeedPage shortPage = service(candidates(1), new MemoryCache(), ads).feed("u1", 0, 2, "corr-short");

        assertFalse(empty.hasNext());
        assertTrue(empty.items().isEmpty());
        assertFalse(shortPage.hasNext());
        assertEquals(1, shortPage.items().size());
    }

    private FeedQueryService service(List<FeedCandidate> candidates, MemoryCache cache, AdInjectionService ads) {
        return new FeedQueryService(
                new StubProjection(candidates),
                cache,
                new PersonalizedRankingStrategy(),
                ads,
                new FeedMixProperties());
    }

    private List<FeedCandidate> candidates(int count) {
        Instant now = Instant.parse("2026-07-13T12:00:00Z");
        List<FeedCandidate> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            FeedPost post = new FeedPost(
                    UUID.randomUUID(),
                    "author-" + index,
                    "text-" + index,
                    "PUBLIC",
                    "PUBLISHED",
                    null,
                    List.of(),
                    List.of(),
                    now.minusSeconds(index),
                    now);
            result.add(new FeedCandidate(post, FeedMetric.empty(post.postId())));
        }
        return result;
    }

    private record StubProjection(List<FeedCandidate> values) implements FeedProjectionPort {
        public void savePost(FeedPost post) { }
        public Optional<FeedPost> findPost(UUID id) { return Optional.empty(); }
        public FeedMetric metric(UUID id) { return FeedMetric.empty(id); }
        public void saveMetric(FeedMetric metric) { }
        public void adjustSignal(String user, UUID post, double weight) { }
        public List<FeedCandidate> candidates(int limit) { return values; }
        public Map<String, Double> authorAffinities(String user) { return Map.of(); }
    }

    private static class MemoryCache implements FeedCachePort {
        private final Map<String, FeedPage> values = new java.util.HashMap<>();
        private FeedPage value;

        public Optional<FeedPage> get(String user, int page, int size) {
            return Optional.ofNullable(values.get(key(user, page, size)));
        }

        public void put(FeedPage page) {
            value = page;
            values.put(key(page.userId(), page.page(), page.size()), page);
        }

        public void invalidate() {
            value = null;
            values.clear();
        }

        private String key(String user, int page, int size) {
            return user + ':' + page + ':' + size;
        }
    }
}
