package com.yeyamo_mobile.api.discovery_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.discovery_service.application.port.*;
import com.yeyamo_mobile.api.discovery_service.domain.model.*;
import com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.SearchAdminService;

@Service 
public class DiscoveryQueryService {
    
    private final DiscoverySearchPort search;
    private final DiscoveryCachePort cache;
    private final AdInjectionService adInjectionService;
    private final SearchAdminService searchAdminService;
    
    @org.springframework.beans.factory.annotation.Autowired
    public DiscoveryQueryService(
            DiscoverySearchPort s,
            DiscoveryCachePort c,
            AdInjectionService adInjectionService,
            SearchAdminService searchAdminService) {
        this.search = s;
        this.cache = c;
        this.adInjectionService = adInjectionService;
        this.searchAdminService = searchAdminService;
    }

    DiscoveryQueryService(DiscoverySearchPort search, DiscoveryCachePort cache, AdInjectionService adInjectionService) {
        this(search, cache, adInjectionService, null);
    }
    
    @Transactional(readOnly = true)
    public DiscoveryPage search(DiscoverySearch criteria) {
        return cache.get(criteria).orElseGet(() -> load(criteria));
    }
    
    @Transactional(readOnly = true)
    public DiscoveryPageWithAds searchWithAds(DiscoverySearch criteria, String userId, String correlationId) {
        // Load organic results (possibly cached)
        DiscoveryPage organicPage = search(criteria);
        
        // Build placement context
        String placementContext = buildPlacementContext(criteria);
        
        // Inject ads
        List<DiscoveryItem> itemsWithAds = adInjectionService.injectAds(
            organicPage.items(),
            userId,
            criteria.page(),
            placementContext,
            correlationId
        );
        
        return new DiscoveryPageWithAds(
            organicPage.page(),
            organicPage.size(),
            organicPage.hasNext(),
            itemsWithAds,
            organicPage.generatedAt()
        );
    }
    
    private DiscoveryPage load(DiscoverySearch c) {
        List<DiscoveryDocument> found = search.search(c, c.size() + 1);
        if (found.isEmpty() && searchAdminService != null) searchAdminService.recordZero(c.query(), c.regionCode());
        boolean next = found.size() > c.size();
        List<DiscoveryDocument> items = next ? List.copyOf(found.subList(0, c.size())) : List.copyOf(found);
        DiscoveryPage p = new DiscoveryPage(c.page(), c.size(), next, items, Instant.now());
        cache.put(c, p);
        return p;
    }
    
    private String buildPlacementContext(DiscoverySearch criteria) {
        StringBuilder context = new StringBuilder("discovery");
        
        if (criteria.type() != null) {
            context.append("_").append(criteria.type().name().toLowerCase());
        }
        if (criteria.categoryCode() != null) {
            context.append("_cat_").append(criteria.categoryCode());
        }
        if (criteria.regionCode() != null) {
            context.append("_region_").append(criteria.regionCode());
        }
        if (criteria.trends()) {
            context.append("_trends");
        }
        
        return context.toString();
    }
}
