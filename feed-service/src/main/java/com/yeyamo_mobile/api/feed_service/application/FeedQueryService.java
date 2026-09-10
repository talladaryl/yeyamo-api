package com.yeyamo_mobile.api.feed_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.feed_service.application.port.*;
import com.yeyamo_mobile.api.feed_service.application.ranking.RankingStrategy;
import com.yeyamo_mobile.api.feed_service.domain.model.CultureContentLink;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedCandidate;

@Service 
public class FeedQueryService {
    
    private final FeedProjectionPort projections;
    private final FeedCachePort cache;
    private final RankingStrategy ranking;
    private final AdInjectionService adInjectionService;
    private final FeedMixProperties mix;
    
    public FeedQueryService(
            FeedProjectionPort p,
            FeedCachePort c,
            RankingStrategy r,
            AdInjectionService adInjectionService, FeedMixProperties mix) {
        this.projections = p;
        this.cache = c;
        this.ranking = r;
        this.adInjectionService = adInjectionService;
        this.mix = mix;
    }
 
    @Transactional(readOnly = true)
    public FeedPage feed(String user, int page, int size) {
        return feed(user, page, size, UUID.randomUUID().toString());
    }
    
    @Transactional(readOnly = true)
    public FeedPage feed(String user, int page, int size, String correlationId) {
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(50, size));
        
        // Try cache first
        Optional<FeedPage> cached = cache.get(user, p, s);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // Build organic feed
        FeedPage organicFeed = buildOrganic(user, p, s);
        
        // Inject ads if enabled (does not modify cache)
        List<FeedItem> itemsWithAds = adInjectionService.injectAds(
            organicFeed.items(),
            user,
            p,
            correlationId
        );
        
        // Return combined feed (organic items were cached, ads are dynamic)
        return new FeedPage(user, p, s, itemsWithAds, organicFeed.generatedAt());
    }

    @Transactional(readOnly = true)
    public PublicFeedPage publicFeed(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        int candidateLimit = Math.min(1000, Math.max(200, (safePage + 1) * safeSize + 1));
        List<PublicFeedItem> candidates = projections.candidates(candidateLimit).stream()
                .filter(candidate -> "PUBLIC".equals(candidate.post().visibility()))
                .filter(candidate -> "PUBLISHED".equals(candidate.post().status()))
                .sorted(Comparator.comparing((FeedCandidate candidate) -> candidate.post().publishedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(candidate -> candidate.post().postId()))
                .map(candidate -> PublicFeedItem.from(candidate.post(), candidate.metric()))
                .toList();
        int from = Math.min(safePage * safeSize, candidates.size());
        int to = Math.min(from + safeSize, candidates.size());
        return new PublicFeedPage(safePage, safeSize, to < candidates.size(), List.copyOf(candidates.subList(from, to)), Instant.now());
    }

    @Transactional(readOnly = true)
    public PublicFeedPage publicFeedByAuthor(String authorId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        int candidateLimit = Math.min(1000, Math.max(200, (safePage + 1) * safeSize + 1));
        List<PublicFeedItem> candidates = projections.candidatesByAuthor(authorId, candidateLimit).stream()
                .filter(candidate -> "PUBLIC".equals(candidate.post().visibility()))
                .filter(candidate -> "PUBLISHED".equals(candidate.post().status()))
                .map(candidate -> PublicFeedItem.from(candidate.post(), candidate.metric()))
                .toList();
        int from = Math.min(safePage * safeSize, candidates.size());
        int to = Math.min(from + safeSize, candidates.size());
        return new PublicFeedPage(safePage, safeSize, to < candidates.size(), List.copyOf(candidates.subList(from, to)), Instant.now());
    }
 
    private FeedPage buildOrganic(String user, int page, int size) {
        Instant now = Instant.now();
        Map<String, Double> affinity = projections.authorAffinities(user);
        int candidateLimit = Math.min(1000, Math.max(200, (page + 1) * size * 10));
        
        List<FeedItem> ranked = projections.candidates(candidateLimit).stream()
            .map(c -> organicItem(c, ranking.score(c, affinity, now) * mix.weight(c.post().referenceType())))
            .sorted(Comparator.comparingDouble(FeedItem::rankingScore).reversed()
                .thenComparing(FeedItem::publishedAt, Comparator.reverseOrder()))
            .toList();
        
        int from = Math.min(page * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        
        FeedPage result = new FeedPage(user, page, size, List.copyOf(ranked.subList(from, to)), now);
        cache.put(result);
        return result;
    }
 
    private FeedItem organicItem(FeedCandidate c, double score) {
        var p = c.post();
        var m = c.metric();
        FeedItem item = FeedItem.organic(
            p.postId(),
            p.authorId(),
            p.caption(),
            p.catalogAssetId(),
            cardType(p.referenceType()),
            p.referenceType(),
            p.referenceId(),
            p.mediaIds(),
            p.hashtags(),
            p.publishedAt(),
            m.likes(),
            m.comments(),
            m.shares(),
            Math.round(score * 100d) / 100d
        );
        if (!"CULTURE_CONTENT".equals(p.referenceType()) || p.referenceId() == null) return item;
        try {
            return projections.findCultureContent(UUID.fromString(p.referenceId()))
                    .filter(CultureContentLink::active)
                    .map(link -> item.withLinkedContent(new FeedItem.FeedLinkedContent(link.type(), link.contentId(), link.title())))
                    .orElse(item);
        } catch (IllegalArgumentException ignored) {
            return item;
        }
    }

    private String cardType(String referenceType) {
        return switch (referenceType == null ? "NONE" : referenceType) {
            case "ARTWORK" -> "ARTWORK";
            case "CULTURE_CONTENT", "LANGUAGE_LESSON" -> "CULTURE_CONTENT";
            case "CULTURE_CHALLENGE" -> "CULTURE_CHALLENGE";
            case "ARTISAN" -> "ARTISAN_SPOTLIGHT";
            default -> "SOCIAL";
        };
    }
}
