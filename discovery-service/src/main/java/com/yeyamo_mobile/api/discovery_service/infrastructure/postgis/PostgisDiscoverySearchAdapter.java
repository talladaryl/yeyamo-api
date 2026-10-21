package com.yeyamo_mobile.api.discovery_service.infrastructure.postgis;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.application.port.DiscoverySearchPort;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;

@Component
@ConditionalOnProperty(name = "discovery.search.engine", havingValue = "postgis", matchIfMissing = true)
public class PostgisDiscoverySearchAdapter implements DiscoverySearchPort {
    private final SpringDiscoveryDocumentRepository documents;
    private final PendingTrendRepository pending;

    public PostgisDiscoverySearchAdapter(SpringDiscoveryDocumentRepository documents, PendingTrendRepository pending) {
        this.documents = documents;
        this.pending = pending;
    }

    @Override
    public void upsert(DiscoveryDocument document) {
        DiscoveryDocumentEntity entity = documents.findBySourceId(document.sourceId())
                .orElseGet(DiscoveryDocumentEntity::new);
        double score = entity.getId() == null ? document.trendScore() : entity.getTrendScore();
        PendingTrendEntity waiting = pending.findById(document.sourceId()).orElse(null);
        if (waiting != null) {
            score += waiting.getScore();
            pending.delete(waiting);
        }

        entity.setId(entity.getId() == null ? document.id() : entity.getId());
        entity.setSourceId(document.sourceId());
        entity.setType(document.type());
        entity.setTitle(document.title());
        entity.setDescription(document.description());
        entity.setCategoryCode(document.categoryCode());
        entity.setRegionCode(document.regionCode());
        entity.setCity(document.city());
        entity.setCountryCode(document.countryCode());
        entity.setAdminLevel1Id(document.adminLevel1Id());
        entity.setCityId(document.cityId());
        entity.setTranslatedTitlesJson(document.translatedTitlesJson());
        entity.setLanguageCodes(document.languageCodes());
        entity.setCommunity(document.community());
        entity.setTags(document.tags());
        entity.setMaterials(document.materials());
        entity.setTechniques(document.techniques());
        entity.setArtisanId(document.artisanId());
        entity.setVerificationStatus(document.verificationStatus());
        entity.setAvailabilityStatus(document.availabilityStatus());
        entity.setPriceMin(document.priceMin());
        entity.setPriceMax(document.priceMax());
        entity.setPopularitySignal(document.popularitySignal());
        entity.setLaunchVisibility(document.launchVisibility() == null ? "PUBLIC" : document.launchVisibility());
        entity.setAliases(document.aliases());
        entity.setLatitude(document.latitude());
        entity.setLongitude(document.longitude());
        entity.setAuthorId(document.authorId());
        entity.setTrendScore(Math.max(0, score));
        entity.setActive(document.active());
        entity.setPublishedAt(document.publishedAt());
        entity.setUpdatedAt(document.updatedAt());
        documents.save(entity);
    }

    @Override
    public Optional<DiscoveryDocument> findBySourceId(String id) {
        return documents.findBySourceId(id).map(this::domain);
    }

    @Override
    public void adjustTrend(String id, double delta) {
        if (documents.adjust(id, delta, Instant.now()) == 0) {
            PendingTrendEntity entity = pending.findById(id).orElseGet(() -> {
                PendingTrendEntity value = new PendingTrendEntity();
                value.setSourceId(id);
                value.setScore(0);
                return value;
            });
            entity.setScore(Math.max(0, entity.getScore() + delta));
            entity.setUpdatedAt(Instant.now());
            pending.save(entity);
        }
    }

    @Override
    public List<DiscoveryDocument> search(DiscoverySearch search, int fetch) {
        String query = blank(search.query());
        String type = search.type() == null ? null : search.type().name();
        String category = blank(search.categoryCode());
        String region = blank(search.regionCode());
        String country = blank(search.countryCode());
        String countries = search.countries().isEmpty() ? null
                : search.countries().stream().map(this::blank).filter(value -> value != null)
                        .map(String::toUpperCase).sorted().collect(Collectors.joining(","));
        int offset = search.page() * search.size();

        List<DiscoveryDocumentEntity> found;
        if (search.latitude() == null) {
            found = documents.search(query, type, category, region, country, countries,
                    blank(search.adminLevel1Id()), blank(search.cityId()), blank(search.languageCode()),
                    blank(search.cultureType()), blank(search.materialId()), blank(search.techniqueId()),
                    search.availability(), search.verified(), search.trends(), fetch, offset);
        } else {
            found = documents.searchGeo(query, type, category, region, country, countries,
                    blank(search.adminLevel1Id()), blank(search.cityId()), blank(search.languageCode()),
                    blank(search.cultureType()), blank(search.materialId()), blank(search.techniqueId()),
                    search.availability(), search.verified(), search.trends(), search.latitude(), search.longitude(),
                    (search.radiusKm() == null ? 25 : search.radiusKm()) * 1000, fetch, offset);
        }
        return found.stream().map(this::domain).toList();
    }

    private DiscoveryDocument domain(DiscoveryDocumentEntity entity) {
        return new DiscoveryDocument(entity.getId(), entity.getSourceId(), entity.getType(), entity.getTitle(),
                entity.getDescription(), entity.getCategoryCode(), entity.getRegionCode(), entity.getCity(),
                entity.getLatitude(), entity.getLongitude(), entity.getAuthorId(), entity.getTrendScore(),
                entity.isActive(), entity.getPublishedAt(), entity.getUpdatedAt(), entity.getCountryCode(),
                entity.getAdminLevel1Id(), entity.getCityId(), entity.getTranslatedTitlesJson(),
                entity.getLanguageCodes(), entity.getCommunity(), entity.getTags(), entity.getMaterials(),
                entity.getTechniques(), entity.getArtisanId(), entity.getVerificationStatus(),
                entity.getAvailabilityStatus(), entity.getPriceMin(), entity.getPriceMax(),
                entity.getPopularitySignal(), entity.getLaunchVisibility(), entity.getAliases());
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
