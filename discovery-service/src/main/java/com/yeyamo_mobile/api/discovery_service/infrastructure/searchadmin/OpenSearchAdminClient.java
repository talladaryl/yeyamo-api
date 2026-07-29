package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.util.*;import com.fasterxml.jackson.databind.JsonNode;import org.springframework.beans.factory.annotation.Value;import org.springframework.http.MediaType;import org.springframework.stereotype.Component;import org.springframework.web.client.RestClient;
@Component public class OpenSearchAdminClient {
 private final RestClient client;private final String index;
 public OpenSearchAdminClient(@Value("${discovery.opensearch.base-url:http://localhost:9200}")String url,@Value("${discovery.opensearch.index:yeyamo-discovery-v1}")String index){client=RestClient.create(url);this.index=index;}
 public JsonNode health(){return client.get().uri("/_cluster/health").retrieve().body(JsonNode.class);}
 public JsonNode indexes(){return client.get().uri("/_cat/indices?format=json&bytes=b").retrieve().body(JsonNode.class);}
 public void reindex(){client.post().uri("/{index}/_update_by_query?conflicts=proceed&refresh=true",index).contentType(MediaType.APPLICATION_JSON).body(Map.of("query",Map.of("match_all",Map.of()))).retrieve().toBodilessEntity();}
}
