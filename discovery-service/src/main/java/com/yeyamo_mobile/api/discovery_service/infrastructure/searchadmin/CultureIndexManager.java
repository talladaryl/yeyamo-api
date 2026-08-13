package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and manages the four OpenSearch indexes introduced for Culture & Artisan.
 *
 * <p>Index names:
 * <ul>
 *   <li>{@code culture_contents_v1}</li>
 *   <li>{@code artworks_v1}</li>
 *   <li>{@code artisans_v1}</li>
 *   <li>{@code languages_v1}</li>
 * </ul>
 * </p>
 *
 * <p>Mappings cover the full document contract specified in the product requirements.
 * They use:
 * <ul>
 *   <li>A shared {@code text} analyser ({@code yeyamo_multilingual}) with
 *       asciifolding + lowercase filters to handle accented characters.</li>
 *   <li>{@code keyword} sub-fields on all filter targets.</li>
 *   <li>{@code geo_point} for coordinates.</li>
 * </ul>
 * </p>
 */
@Component
public class CultureIndexManager {

    private static final Logger log = LoggerFactory.getLogger(CultureIndexManager.class);

    static final String CULTURE_CONTENTS = "culture_contents_v1";
    static final String ARTWORKS         = "artworks_v1";
    static final String ARTISANS         = "artisans_v1";
    static final String LANGUAGES        = "languages_v1";

    public static final List<String> ALL_CULTURE_INDEXES =
            List.of(CULTURE_CONTENTS, ARTWORKS, ARTISANS, LANGUAGES);

    private final RestClient client;

    public CultureIndexManager(
            @Value("${discovery.opensearch.base-url:http://localhost:9200}") String baseUrl) {
        this.client = RestClient.create(baseUrl);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Creates all four indexes if they do not exist. Idempotent. */
    public void ensureIndexes() {
        ALL_CULTURE_INDEXES.forEach(this::ensureIndex);
    }

    public void ensureIndex(String indexName) {
        try {
            client.get().uri("/{index}", indexName).retrieve().toBodilessEntity();
            log.debug("Index {} already exists", indexName);
        } catch (Exception e) {
            createIndex(indexName);
        }
    }

    /** Trigger reindex (update_by_query) for a single index. */
    public void reindex(String indexName) {
        log.info("Triggering reindex for index: {}", indexName);
        client.post()
              .uri("/{index}/_update_by_query?conflicts=proceed&refresh=true", indexName)
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("query", Map.of("match_all", Map.of())))
              .retrieve()
              .toBodilessEntity();
    }

    // -------------------------------------------------------------------------
    // Index creation
    // -------------------------------------------------------------------------

    private void createIndex(String name) {
        Map<String, Object> body = buildIndexBody(name);
        try {
            client.put()
                  .uri("/{index}", name)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(body)
                  .retrieve()
                  .toBodilessEntity();
            log.info("Created OpenSearch index: {}", name);
        } catch (Exception ex) {
            log.error("Failed to create index {}: {}", name, ex.getMessage());
        }
    }

    private Map<String, Object> buildIndexBody(String name) {
        return Map.of(
                "settings", settings(),
                "mappings", Map.of("properties", propertiesFor(name))
        );
    }

    // -------------------------------------------------------------------------
    // Settings — shared multilingual analyser
    // -------------------------------------------------------------------------

    private Map<String, Object> settings() {
        return Map.of(
                "number_of_shards",   1,
                "number_of_replicas", 1,
                "analysis", Map.of(
                        "analyzer", Map.of(
                                "yeyamo_multilingual", Map.of(
                                        "type",        "custom",
                                        "tokenizer",   "standard",
                                        "filter",      List.of("lowercase", "asciifolding", "word_delimiter_graph")
                                )
                        )
                )
        );
    }

    // -------------------------------------------------------------------------
    // Mappings per index
    // -------------------------------------------------------------------------

    private Map<String, Object> propertiesFor(String name) {
        // Core fields shared by all culture indexes
        Map<String, Object> props = new LinkedHashMap<>(sharedFields());

        switch (name) {
            case ARTWORKS -> {
                props.put("artisanId",           keyword());
                props.put("materials",           analyzed());
                props.put("techniques",          analyzed());
                props.put("verificationStatus",  keyword());
                props.put("availabilityStatus",  keyword());
                props.put("priceMin",            floatField());
                props.put("priceMax",            floatField());
            }
            case ARTISANS -> {
                props.put("specialties",         analyzed());
                props.put("verificationStatus",  keyword());
                props.put("followersCount",      Map.of("type", "integer"));
            }
            case LANGUAGES -> {
                props.put("nativeName",          analyzed());
                props.put("iso639Code",          keyword());
                props.put("scriptCode",          keyword());
                props.put("learnerCount",        Map.of("type", "integer"));
            }
            case CULTURE_CONTENTS -> {
                props.put("contentType",         keyword());
                props.put("verificationStatus",  keyword());
            }
            default -> { /* nothing extra */ }
        }

        return props;
    }

    /** Fields present on all four culture indexes. */
    private Map<String, Object> sharedFields() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                   keyword());
        m.put("sourceId",             keyword());
        m.put("type",                 keyword());
        m.put("active",               Map.of("type", "boolean"));
        m.put("contentType",          keyword());
        m.put("launchVisibility",     keyword());
        // textual — multilingual analyser + keyword sub-field for exact filters
        m.put("title",                textWithKeyword());
        m.put("description",          analyzedOnly());
        m.put("translatedTitlesJson", analyzedOnly());   // searchable blob of all translations
        m.put("aliases",              analyzedOnly());
        m.put("summary",              analyzedOnly());
        // filterable strings
        m.put("countryCode",          keyword());
        m.put("adminLevel1Id",        keyword());
        m.put("cityId",               keyword());
        m.put("community",            textWithKeyword());
        m.put("languageCodes",        analyzed());        // comma-sep; match query
        m.put("tags",                 analyzed());
        m.put("categoryCode",         keyword());
        m.put("regionCode",           keyword());
        m.put("city",                 textWithKeyword());
        // geo
        m.put("location",             Map.of("type", "geo_point"));
        m.put("coordinates",          Map.of("type", "geo_point"));
        // signals
        m.put("trendScore",           floatField());
        m.put("popularitySignal",     floatField());
        // dates
        m.put("publishedAt",          Map.of("type", "date"));
        m.put("updatedAt",            Map.of("type", "date"));
        return m;
    }

    // -------------------------------------------------------------------------
    // Field type helpers
    // -------------------------------------------------------------------------

    /** Keyword only (exact match, filter, aggregation). */
    private Map<String, Object> keyword() {
        return Map.of("type", "keyword");
    }

    /** Text with asciifolding analyser + keyword sub-field for sort/filter. */
    private Map<String, Object> textWithKeyword() {
        return Map.of(
                "type",     "text",
                "analyzer", "yeyamo_multilingual",
                "fields",   Map.of("keyword", Map.of("type", "keyword", "ignore_above", 256))
        );
    }

    /** Text analysed only (no keyword sub-field). */
    private Map<String, Object> analyzedOnly() {
        return Map.of("type", "text", "analyzer", "yeyamo_multilingual");
    }

    /** Comma-separated values analysed as text. */
    private Map<String, Object> analyzed() {
        return analyzedOnly();
    }

    private Map<String, Object> floatField() {
        return Map.of("type", "float");
    }
}
