package com.yeyamo_mobile.api.discovery_service.infrastructure.opensearch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.application.port.DiscoverySearchPort;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;

/**
 * OpenSearch search adapter extended for Culture & Artisan fields.
 *
 * <p>Multilingal search is handled via:
 * <ul>
 *   <li>{@code title} — primary language (indexed with {@code analysis.analyzer})</li>
 *   <li>{@code translatedTitlesJson} — stored as flat text; OpenSearch's
 *       {@code match_phrase} picks it up via the default {@code _all}-style
 *       {@code multi_match}</li>
 *   <li>{@code languageCodes} — term filter when {@code languageCode} is supplied</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "discovery.search.engine", havingValue = "opensearch")
public class OpenSearchDiscoverySearchAdapter implements DiscoverySearchPort {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String index;

    public OpenSearchDiscoverySearchAdapter(
            RestClient.Builder builder,
            ObjectMapper mapper,
            @Value("${discovery.opensearch.base-url:http://localhost:9200}") String baseUrl,
            @Value("${discovery.opensearch.index:yeyamo-discovery-v1}") String index) {
        this.client = builder.baseUrl(baseUrl).build();
        this.mapper = mapper;
        this.index  = index;
    }

    // -------------------------------------------------------------------------
    // DiscoverySearchPort
    // -------------------------------------------------------------------------

    @Override
    public void upsert(DiscoveryDocument document) {
        client.put()
              .uri("/{index}/_doc/{id}", index, document.sourceId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(toSource(document))
              .retrieve()
              .toBodilessEntity();
    }

    @Override
    public Optional<DiscoveryDocument> findBySourceId(String sourceId) {
        try {
            JsonNode result = client.get()
                    .uri("/{index}/_doc/{id}", index, sourceId)
                    .retrieve()
                    .body(JsonNode.class);
            return result == null || !result.path("found").asBoolean(true)
                    ? Optional.empty()
                    : Optional.of(toDomain(result.path("_source")));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public void adjustTrend(String sourceId, double delta) {
        findBySourceId(sourceId).ifPresent(d -> upsert(
                withTrend(d, Math.max(0, d.trendScore() + delta))));
    }

    @Override
    public List<DiscoveryDocument> search(DiscoverySearch criteria, int fetchSize) {
        Map<String, Object> body = buildQuery(criteria, fetchSize);
        JsonNode response = client.post()
                .uri("/{index}/_search", index)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) return List.of();
        List<DiscoveryDocument> result = new ArrayList<>();
        response.path("hits").path("hits")
                .forEach(hit -> result.add(toDomain(hit.path("_source"))));
        return result;
    }

    // -------------------------------------------------------------------------
    // Query builder
    // -------------------------------------------------------------------------

    private Map<String, Object> buildQuery(DiscoverySearch c, int fetchSize) {
        List<Object> filter = new ArrayList<>();
        List<Object> must   = new ArrayList<>();

        // mandatory
        filter.add(Map.of("term", Map.of("active", true)));

        // -- type --
        if (c.type() != null)
            filter.add(Map.of("term", Map.of("type", c.type().name())));

        // -- category / region (legacy) --
        if (text(c.categoryCode()) != null)
            filter.add(Map.of("term", Map.of("categoryCode.keyword", c.categoryCode())));
        if (text(c.regionCode()) != null)
            filter.add(Map.of("term", Map.of("regionCode.keyword", c.regionCode())));

        // -- culture & artisan filters --
        if (text(c.countryCode()) != null)
            filter.add(Map.of("term", Map.of("countryCode.keyword", c.countryCode())));
        if (text(c.adminLevel1Id()) != null)
            filter.add(Map.of("term", Map.of("adminLevel1Id.keyword", c.adminLevel1Id())));
        if (text(c.cityId()) != null)
            filter.add(Map.of("term", Map.of("cityId.keyword", c.cityId())));
        if (text(c.languageCode()) != null)
            // languageCodes is a comma-separated field; use match instead of term
            filter.add(Map.of("match", Map.of("languageCodes", c.languageCode())));
        if (text(c.cultureType()) != null)
            filter.add(Map.of("term", Map.of("cultureType.keyword", c.cultureType())));
        if (text(c.materialId()) != null)
            filter.add(Map.of("match", Map.of("materials", c.materialId())));
        if (text(c.techniqueId()) != null)
            filter.add(Map.of("match", Map.of("techniques", c.techniqueId())));
        if (c.availability() != null)
            filter.add(Map.of("term", Map.of(
                    "availabilityStatus.keyword", c.availability() ? "AVAILABLE" : "SOLD")));
        if (c.verified() != null && c.verified())
            filter.add(Map.of("term", Map.of("verificationStatus.keyword", "VERIFIED")));

        // -- geo --
        if (c.latitude() != null)
            filter.add(Map.of("geo_distance", Map.of(
                    "distance",  (c.radiusKm() == null ? 25 : c.radiusKm()) + "km",
                    "location",  Map.of("lat", c.latitude(), "lon", c.longitude()))));

        // -- full-text (multilingal: searches title, translatedTitlesJson, description, city) --
        if (text(c.query()) != null)
            must.add(Map.of("multi_match", Map.of(
                    "query",    c.query(),
                    "fields",   List.of("title^4", "translatedTitlesJson^3",
                                        "description", "city^2", "tags", "community"),
                    "fuzziness", "AUTO",
                    "operator",  "or")));

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", filter);
        if (!must.isEmpty()) bool.put("must", must);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", c.page() * c.size());
        body.put("size", fetchSize);
        body.put("query", Map.of("bool", bool));
        body.put("sort", c.trends()
                ? List.of(Map.of("popularitySignal", "desc"), Map.of("trendScore", "desc"), Map.of("publishedAt", "desc"))
                : List.of("_score", Map.of("publishedAt", "desc")));

        return body;
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> toSource(DiscoveryDocument d) {
        Map<String, Object> m = mapper.convertValue(d, Map.class);
        if (d.latitude() != null)
            m.put("location", Map.of("lat", d.latitude(), "lon", d.longitude()));
        return m;
    }

    private DiscoveryDocument toDomain(JsonNode n) {
        return new DiscoveryDocument(
                UUID.fromString(n.path("id").asText()),
                n.path("sourceId").asText(),
                DiscoveryType.valueOf(n.path("type").asText()),
                n.path("title").asText(),
                str(n, "description"),
                str(n, "categoryCode"),
                str(n, "regionCode"),
                str(n, "city"),
                dbl(n, "latitude"),
                dbl(n, "longitude"),
                str(n, "authorId"),
                n.path("trendScore").asDouble(),
                n.path("active").asBoolean(),
                instant(n, "publishedAt"),
                instant(n, "updatedAt"),
                // culture extensions
                str(n, "countryCode"),
                str(n, "adminLevel1Id"),
                str(n, "cityId"),
                str(n, "translatedTitlesJson"),
                str(n, "languageCodes"),
                str(n, "community"),
                str(n, "tags"),
                str(n, "materials"),
                str(n, "techniques"),
                str(n, "artisanId"),
                str(n, "verificationStatus"),
                str(n, "availabilityStatus"),
                bd(n, "priceMin"),
                bd(n, "priceMax"),
                n.path("popularitySignal").asDouble(0)
        );
    }

    private DiscoveryDocument withTrend(DiscoveryDocument d, double score) {
        return new DiscoveryDocument(
                d.id(), d.sourceId(), d.type(), d.title(), d.description(),
                d.categoryCode(), d.regionCode(), d.city(), d.latitude(), d.longitude(),
                d.authorId(), score, d.active(), d.publishedAt(), Instant.now(),
                d.countryCode(), d.adminLevel1Id(), d.cityId(),
                d.translatedTitlesJson(), d.languageCodes(), d.community(), d.tags(),
                d.materials(), d.techniques(), d.artisanId(),
                d.verificationStatus(), d.availabilityStatus(),
                d.priceMin(), d.priceMax(), d.popularitySignal());
    }

    private String str(JsonNode n, String f) { JsonNode v = n.get(f); return (v == null || v.isNull()) ? null : v.asText(); }
    private Double dbl(JsonNode n, String f)  { JsonNode v = n.get(f); return (v == null || v.isNull()) ? null : v.asDouble(); }
    private BigDecimal bd(JsonNode n, String f) { JsonNode v = n.get(f); return (v == null || v.isNull()) ? null : v.decimalValue(); }
    private Instant instant(JsonNode n, String f) { String s = str(n, f); return s == null ? null : Instant.parse(s); }
    private String text(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}
