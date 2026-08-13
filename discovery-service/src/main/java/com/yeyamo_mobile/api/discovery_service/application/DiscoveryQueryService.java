package com.yeyamo_mobile.api.discovery_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.discovery_service.application.port.*;
import com.yeyamo_mobile.api.discovery_service.domain.model.*;
import com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.SearchAdminService;
import com.yeyamo_mobile.shared.country.CountryConfigClient;

@Service 
public class DiscoveryQueryService {
    
    private final DiscoverySearchPort search;
    private final DiscoveryCachePort cache;
    private final AdInjectionService adInjectionService;
    private final SearchAdminService searchAdminService;
    private final CountryConfigClient countries;
    private final boolean publicHeritageVisibleWhenDisabled;
    
    @org.springframework.beans.factory.annotation.Autowired
    public DiscoveryQueryService(
            DiscoverySearchPort s,
            DiscoveryCachePort c,
            AdInjectionService adInjectionService,
            SearchAdminService searchAdminService,
            CountryConfigClient countries,
            @org.springframework.beans.factory.annotation.Value("${discovery.disabled-country.public-heritage-visible:false}") boolean publicHeritageVisibleWhenDisabled) {
        this.search = s;
        this.cache = c;
        this.adInjectionService = adInjectionService;
        this.searchAdminService = searchAdminService;
        this.countries = countries;
        this.publicHeritageVisibleWhenDisabled = publicHeritageVisibleWhenDisabled;
    }

    public DiscoveryQueryService(DiscoverySearchPort s, DiscoveryCachePort c, AdInjectionService adInjectionService, SearchAdminService searchAdminService) {
        this(s, c, adInjectionService, searchAdminService, null, false);
    }

    DiscoveryQueryService(DiscoverySearchPort search, DiscoveryCachePort cache, AdInjectionService adInjectionService) {
        this(search, cache, adInjectionService, null, null, false);
    }
    
    @Transactional(readOnly = true)
    public DiscoveryPage search(DiscoverySearch criteria) {
        DiscoverySearch effective = normalizeTerritory(criteria);
        if (disabledCountry(effective)) return new DiscoveryPage(effective.page(), effective.size(), false, List.of(), Instant.now());
        return cache.get(effective).orElseGet(() -> load(effective));
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

    private DiscoverySearch normalizeTerritory(DiscoverySearch c) {
        if (c.scope() == DiscoveryScope.AFRICA) {
            return new DiscoverySearch(c.query(), c.type(), c.categoryCode(), c.regionCode(), c.latitude(), c.longitude(), c.radiusKm(),
                    c.page(), c.size(), c.trends(), null, c.adminLevel1Id(), null, c.languageCode(), c.cultureType(),
                    c.materialId(), c.techniqueId(), c.availability(), c.verified(), c.countries(), c.scope());
        }
        if (c.scope() == DiscoveryScope.LOCAL && (c.countryCode() == null || c.cityId() == null))
            throw new IllegalArgumentException("LOCAL scope requires countryCode and cityId");
        return c;
    }

    private boolean disabledCountry(DiscoverySearch c) {
        if (countries == null || c.countryCode() == null) return false;
        try {
            return "DISABLED".equals(countries.getCountry(c.countryCode()).launchStatus()) && !publicHeritageVisibleWhenDisabled;
        } catch (CountryConfigClient.CountryConfigException exception) {
            throw new IllegalStateException("Country configuration is required to search this country", exception);
        }
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
