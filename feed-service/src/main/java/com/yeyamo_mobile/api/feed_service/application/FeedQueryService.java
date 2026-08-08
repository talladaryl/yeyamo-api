package com.yeyamo_mobile.api.feed_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.feed_service.application.port.*;
import com.yeyamo_mobile.api.feed_service.application.ranking.RankingStrategy;
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
        return FeedItem.organic(
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
