package com.yeyamo_mobile.api.discovery_service.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryProjectionService;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;

@Component
public class DiscoveryEventConsumer {
    private final ObjectMapper mapper;
    private final DiscoveryProjectionService projections;
    private final ProcessedEventRepository processed;
    public DiscoveryEventConsumer(ObjectMapper mapper, DiscoveryProjectionService projections, ProcessedEventRepository processed) {
        this.mapper = mapper; this.projections = projections; this.processed = processed;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.catalog-events:catalog.events}", groupId = "${spring.kafka.consumer.group-id:discovery-service}")
    @Transactional public void catalog(String raw) throws Exception {
        JsonNode event = event(raw); UUID eventId = id(event); if (processed.existsById(eventId)) return;
        String eventType = required(event, "eventType");
        if (!eventType.startsWith("catalog.asset.") || event.path("eventVersion").asInt() != 1) throw new IllegalArgumentException("Unsupported catalog event contract");
        JsonNode p = event.path("payload"); String assetId = required(p, "assetId"); String sourceId = "catalog:" + assetId;
        var current = projections.find(sourceId).orElse(null);
        String title = text(p, "name", current == null ? "Catalogue" : current.title());
        DiscoveryType type = type(text(p, "type", current == null ? "PLACE" : current.type().name()));
        boolean active = !eventType.endsWith("deleted") && "PUBLISHED".equals(text(p, "status", "DRAFT"));
        projections.project(new DiscoveryDocument(stableId(sourceId), sourceId, type, title,
                current == null ? null : current.description(), text(p,"categoryCode", null), text(p,"regionCode", null), text(p,"city", null),
                number(p,"latitude"), number(p,"longitude"), text(p,"ownerId", null), current == null ? 0 : current.trendScore(), active,
                active ? (current == null || current.publishedAt() == null ? occurred(event) : current.publishedAt()) : current == null ? null : current.publishedAt(), Instant.now()));
        receipt(eventId, eventType);
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.content-events:content.events}", groupId = "${spring.kafka.consumer.group-id:discovery-service}")
    @Transactional public void content(String raw) throws Exception {
        JsonNode event = event(raw); UUID eventId = id(event); if (processed.existsById(eventId)) return;
        String eventType = required(event, "eventType");
        if (!eventType.startsWith("content.post.") || event.path("eventVersion").asInt() != 1) throw new IllegalArgumentException("Unsupported content event contract");
        JsonNode p = event.path("payload"); String postId = required(p, "postId"); String sourceId = "content:" + postId;
        var current = projections.find(sourceId).orElse(null);
        String title = text(p, "caption", current == null ? "Publication" : current.title());
        boolean active = !eventType.endsWith("deleted") && "PUBLISHED".equals(text(p,"status","DRAFT")) && "PUBLIC".equals(text(p,"visibility","PRIVATE"));
        Instant published = instant(p, "publishedAt", active ? occurred(event) : current == null ? null : current.publishedAt());
        projections.project(new DiscoveryDocument(stableId(sourceId), sourceId, DiscoveryType.CONTENT, title, hashtags(p), null, null, null,
                null, null, text(p,"authorId", null), current == null ? 0 : current.trendScore(), active, published, Instant.now()));
        receipt(eventId, eventType);
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.interaction-events:interaction.events}", groupId = "${spring.kafka.consumer.group-id:discovery-service}")
    @Transactional public void interaction(String raw) throws Exception {
        JsonNode event = event(raw); UUID eventId = id(event); if (processed.existsById(eventId)) return;
        String eventType = required(event, "eventType"); JsonNode p = event.path("payload");
        if (!eventType.startsWith("interaction.")) throw new IllegalArgumentException("Unsupported interaction event contract");
        String sourceId=eventType.equals("interaction.checkin.created")?"catalog:"+required(p,"catalogAssetId"):"content:"+required(p,"postId");
        projections.interaction(eventType, sourceId);
        receipt(eventId, eventType);
    }

    private JsonNode event(String raw) throws Exception { return mapper.readTree(raw); }
    private UUID id(JsonNode event) { return UUID.fromString(required(event,"eventId")); }
    private void receipt(UUID id, String type) { ProcessedEventEntity e = new ProcessedEventEntity(); e.setEventId(id); e.setEventType(type); e.setProcessedAt(Instant.now()); processed.save(e); }
    private String required(JsonNode n,String f) { String v=text(n,f,null); if(v==null||v.isBlank()) throw new IllegalArgumentException(f+" is required"); return v; }
    private String text(JsonNode n,String f,String fallback) { JsonNode v=n.get(f); return v==null||v.isNull()||v.asText().isBlank()?fallback:v.asText(); }
    private Double number(JsonNode n,String f) { JsonNode v=n.get(f); return v==null||v.isNull()||!v.isNumber()?null:v.asDouble(); }
    private Instant occurred(JsonNode e) { return instant(e,"occurredAt",Instant.now()); }
    private Instant instant(JsonNode n,String f,Instant fallback) { try { String v=text(n,f,null); return v==null?fallback:Instant.parse(v); } catch(RuntimeException ex) { return fallback; } }
    private String hashtags(JsonNode payload) { JsonNode values=payload.path("hashtags"); if(!values.isArray())return null; List<String> result=new ArrayList<>(); values.forEach(v->result.add("#"+v.asText())); return String.join(" ",result); }
    private UUID stableId(String sourceId) { return UUID.nameUUIDFromBytes(sourceId.getBytes(StandardCharsets.UTF_8)); }
    private DiscoveryType type(String value) { try { return DiscoveryType.valueOf(value.toUpperCase(Locale.ROOT)); } catch(Exception e) { return DiscoveryType.PLACE; } }
}
