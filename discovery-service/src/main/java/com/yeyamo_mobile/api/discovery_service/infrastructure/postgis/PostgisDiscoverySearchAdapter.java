package com.yeyamo_mobile.api.discovery_service.infrastructure.postgis;

import java.time.Instant;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.application.port.DiscoverySearchPort;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;

@Component
@ConditionalOnProperty(name="discovery.search.engine",havingValue="postgis",matchIfMissing=true)
public class PostgisDiscoverySearchAdapter implements DiscoverySearchPort {
    private final SpringDiscoveryDocumentRepository documents; private final PendingTrendRepository pending;
    public PostgisDiscoverySearchAdapter(SpringDiscoveryDocumentRepository documents,PendingTrendRepository pending){this.documents=documents;this.pending=pending;}
    public void upsert(DiscoveryDocument document) {
        DiscoveryDocumentEntity entity=documents.findBySourceId(document.sourceId()).orElseGet(DiscoveryDocumentEntity::new);
        double score=entity.getId()==null?document.trendScore():entity.getTrendScore();
        PendingTrendEntity waiting=pending.findById(document.sourceId()).orElse(null);
        if(waiting!=null){score+=waiting.getScore();pending.delete(waiting);}
        entity.setId(entity.getId()==null?document.id():entity.getId()); entity.setSourceId(document.sourceId()); entity.setType(document.type());
        entity.setTitle(document.title()); entity.setDescription(document.description()); entity.setCategoryCode(document.categoryCode()); entity.setRegionCode(document.regionCode());
        entity.setCity(document.city()); entity.setLatitude(document.latitude()); entity.setLongitude(document.longitude()); entity.setAuthorId(document.authorId());
        entity.setTrendScore(Math.max(0,score)); entity.setActive(document.active()); entity.setPublishedAt(document.publishedAt()); entity.setUpdatedAt(document.updatedAt()); documents.save(entity);
    }
    public Optional<DiscoveryDocument> findBySourceId(String id){return documents.findBySourceId(id).map(this::domain);}
    public void adjustTrend(String id,double delta) {
        if(documents.adjust(id,delta,Instant.now())==0) { PendingTrendEntity entity=pending.findById(id).orElseGet(()->{var value=new PendingTrendEntity();value.setSourceId(id);value.setScore(0);return value;});
            entity.setScore(Math.max(0,entity.getScore()+delta));entity.setUpdatedAt(Instant.now());pending.save(entity); }
    }
    public List<DiscoveryDocument> search(DiscoverySearch search,int fetch) {
        String query=blank(search.query()),type=search.type()==null?null:search.type().name(),category=blank(search.categoryCode()),region=blank(search.regionCode()); int offset=search.page()*search.size();
        var found=search.latitude()==null?documents.search(query,type,category,region,search.trends(),fetch,offset):documents.searchGeo(query,type,category,region,search.trends(),search.latitude(),search.longitude(),(search.radiusKm()==null?25:search.radiusKm())*1000,fetch,offset);
        return found.stream().map(this::domain).toList();
    }
    private DiscoveryDocument domain(DiscoveryDocumentEntity e){return new DiscoveryDocument(e.getId(),e.getSourceId(),e.getType(),e.getTitle(),e.getDescription(),e.getCategoryCode(),e.getRegionCode(),e.getCity(),e.getLatitude(),e.getLongitude(),e.getAuthorId(),e.getTrendScore(),e.isActive(),e.getPublishedAt(),e.getUpdatedAt());}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
}
