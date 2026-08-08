package com.yeyamo_mobile.api.graph_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.graph_service.infrastructure.GraphProjectionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false", "spring.kafka.listener.auto-startup=false",
        "jwt.secret=test-secret-test-secret-test-secret-1234" })
@Testcontainers(disabledWithoutDocker = true)
class GraphProjectionIntegrationTest {
    @Container
    static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.24-community").withoutAuthentication();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
    }

    @Autowired GraphProjectionService projection;
    @Autowired Neo4jClient neo;
    @Autowired ObjectMapper json;

    @Test
    void duplicateEventCreatesUniqueNodesAndRelation() throws Exception {
        UUID eventId = UUID.randomUUID();
        String raw = """
                {"eventId":"%s","eventType":"ArtworkPublished","eventVersion":1,
                "correlationId":"corr-graph-1",
                "payload":{"artworkId":"art-1","artisanPartnerId":"artisan-1","countryCode":"CM","materialIds":["wood"]}}
                """.formatted(eventId);

        assertTrue(projection.apply(json.readTree(raw)));
        assertFalse(projection.apply(json.readTree(raw)));
        long nodes = neo.query("MATCH (a:Artwork {id:'art-1'}) RETURN count(a) c")
                .fetchAs(Long.class).mappedBy((type, row) -> row.get("c").asLong()).one().orElse(0L);
        long relations = neo.query("MATCH (:Artisan {id:'artisan-1'})-[r:CREATED]->(:Artwork {id:'art-1'}) RETURN count(r) c")
                .fetchAs(Long.class).mappedBy((type, row) -> row.get("c").asLong()).one().orElse(0L);
        assertEquals(1, nodes);
        assertEquals(1, relations);
    }
}
