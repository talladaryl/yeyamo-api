package com.yeyamo_mobile.api.discovery_service.infrastructure.opensearch;

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

@Component
@ConditionalOnProperty(name="discovery.search.engine",havingValue="opensearch")
public class OpenSearchDiscoverySearchAdapter implements DiscoverySearchPort {
    private final RestClient client; private final ObjectMapper mapper; private final String index;
    public OpenSearchDiscoverySearchAdapter(RestClient.Builder builder,ObjectMapper mapper,
            @Value("${discovery.opensearch.base-url:http://localhost:9200}") String baseUrl,
            @Value("${discovery.opensearch.index:yeyamo-discovery-v1}") String index) {
        this.client=builder.baseUrl(baseUrl).build(); this.mapper=mapper; this.index=index;
    }
    public void upsert(DiscoveryDocument document) {
        client.put().uri("/{index}/_doc/{id}",index,document.sourceId()).contentType(MediaType.APPLICATION_JSON)
                .body(source(document)).retrieve().toBodilessEntity();
    }
    public Optional<DiscoveryDocument> findBySourceId(String sourceId) {
        try { JsonNode result=client.get().uri("/{index}/_doc/{id}",index,sourceId).retrieve().body(JsonNode.class);
            return result==null||!result.path("found").asBoolean(true)?Optional.empty():Optional.of(domain(result.path("_source")));
        } catch(HttpClientErrorException.NotFound e) { return Optional.empty(); }
    }
    public void adjustTrend(String sourceId,double delta) {
        Optional<DiscoveryDocument> current=findBySourceId(sourceId);
        current.ifPresent(d->upsert(new DiscoveryDocument(d.id(),d.sourceId(),d.type(),d.title(),d.description(),d.categoryCode(),d.regionCode(),d.city(),
                d.latitude(),d.longitude(),d.authorId(),Math.max(0,d.trendScore()+delta),d.active(),d.publishedAt(),Instant.now())));
    }
    public List<DiscoveryDocument> search(DiscoverySearch criteria,int fetchSize) {
        Map<String,Object> bool=new LinkedHashMap<>(); List<Object> filter=new ArrayList<>(); List<Object> must=new ArrayList<>();
        filter.add(Map.of("term",Map.of("active",true)));
        if(criteria.type()!=null)filter.add(Map.of("term",Map.of("type",criteria.type().name())));
        if(text(criteria.categoryCode())!=null)filter.add(Map.of("term",Map.of("categoryCode.keyword",criteria.categoryCode())));
        if(text(criteria.regionCode())!=null)filter.add(Map.of("term",Map.of("regionCode.keyword",criteria.regionCode())));
        if(criteria.latitude()!=null)filter.add(Map.of("geo_distance",Map.of("distance",(criteria.radiusKm()==null?25:criteria.radiusKm())+"km","location",Map.of("lat",criteria.latitude(),"lon",criteria.longitude()))));
        if(text(criteria.query())!=null)must.add(Map.of("multi_match",Map.of("query",criteria.query(),"fields",List.of("title^3","description","city^2"),"fuzziness","AUTO")));
        bool.put("filter",filter); if(!must.isEmpty())bool.put("must",must);
        Map<String,Object> body=new LinkedHashMap<>(); body.put("from",criteria.page()*criteria.size()); body.put("size",fetchSize); body.put("query",Map.of("bool",bool));
        body.put("sort",criteria.trends()?List.of(Map.of("trendScore","desc"),Map.of("publishedAt","desc")):List.of("_score",Map.of("publishedAt","desc")));
        JsonNode response=client.post().uri("/{index}/_search",index).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        if(response==null)return List.of(); List<DiscoveryDocument> result=new ArrayList<>(); response.path("hits").path("hits").forEach(hit->result.add(domain(hit.path("_source")))); return result;
    }
    private Map<String,Object> source(DiscoveryDocument d) { Map<String,Object> value=mapper.convertValue(d,Map.class); if(d.latitude()!=null)value.put("location",Map.of("lat",d.latitude(),"lon",d.longitude())); return value; }
    private DiscoveryDocument domain(JsonNode n) { return new DiscoveryDocument(UUID.fromString(n.path("id").asText()),n.path("sourceId").asText(),DiscoveryType.valueOf(n.path("type").asText()),n.path("title").asText(),nullable(n,"description"),nullable(n,"categoryCode"),nullable(n,"regionCode"),nullable(n,"city"),number(n,"latitude"),number(n,"longitude"),nullable(n,"authorId"),n.path("trendScore").asDouble(),n.path("active").asBoolean(),instant(n,"publishedAt"),instant(n,"updatedAt")); }
    private String nullable(JsonNode n,String f) { JsonNode v=n.get(f); return v==null||v.isNull()?null:v.asText(); }
    private Double number(JsonNode n,String f) { JsonNode v=n.get(f); return v==null||v.isNull()?null:v.asDouble(); }
    private Instant instant(JsonNode n,String f) { String value=nullable(n,f); return value==null?null:Instant.parse(value); }
    private String text(String v) { return v==null||v.isBlank()?null:v.trim(); }
}
