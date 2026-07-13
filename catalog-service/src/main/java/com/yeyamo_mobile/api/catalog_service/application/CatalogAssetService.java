package com.yeyamo_mobile.api.catalog_service.application;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogOutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.*;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogAssetRepository;

@Service @Transactional
public class CatalogAssetService {
    private final CatalogAssetRepository repository;
    private final CatalogOutboxPort outbox;
    public CatalogAssetService(CatalogAssetRepository repository,CatalogOutboxPort outbox){
        this.repository=repository;this.outbox=outbox;
    }
    public CatalogAsset create(AssetType type,UUID ownerId,String name,String requestedSlug,String description,
            String categoryCode,String regionCode,String city,String district,String address,
            double latitude,double longitude,String correlationId,String actorId){
        String slug=slug(requestedSlug,name);
        ensureSlug(slug,null);
        CatalogAsset asset=CatalogAsset.create(type,ownerId,"catalog",null,name,slug,description,
                categoryCode,regionCode,city,district,address,new GeoPoint(latitude,longitude));
        CatalogAsset saved=repository.save(asset);
        outbox.append("catalog.asset.created",saved,correlationId,actorId);
        return saved;
    }
    public CatalogAsset update(UUID id,String name,String requestedSlug,String description,String categoryCode,
            String regionCode,String city,String district,String address,double latitude,double longitude,
            String correlationId,String actorId){
        CatalogAsset asset=getRequired(id);
        String slug=slug(requestedSlug,name); ensureSlug(slug,id);
        asset.update(name,slug,description,categoryCode,regionCode,city,district,address,
                new GeoPoint(latitude,longitude));
        CatalogAsset saved=repository.save(asset);
        outbox.append("catalog.asset.updated",saved,correlationId,actorId);
        return saved;
    }
    public CatalogAsset changeStatus(UUID id,AssetStatus status,String correlationId,String actorId){
        CatalogAsset asset=getRequired(id); asset.changeStatus(status);
        CatalogAsset saved=repository.save(asset);
        outbox.append("catalog.asset.status_changed",saved,correlationId,actorId);
        return saved;
    }
    public void delete(UUID id,String correlationId,String actorId){
        CatalogAsset asset=getRequired(id);asset.delete();CatalogAsset saved=repository.save(asset);
        outbox.append("catalog.asset.deleted",saved,correlationId,actorId);
    }
    public CatalogAsset synchronizeLegacyPlace(String externalId,UUID ownerId,String name,String requestedSlug,
            String description,String categoryCode,String regionCode,String city,String district,String address,
            double latitude,double longitude,AssetStatus status,String correlationId){
        return synchronizeExternalAsset("place-service",externalId,AssetType.PLACE,ownerId,name,requestedSlug,
                description,categoryCode,regionCode,city,district,address,latitude,longitude,status,correlationId,"place-service");
    }
    public CatalogAsset synchronizeExternalAsset(String source,String externalId,AssetType type,UUID ownerId,String name,String requestedSlug,
            String description,String categoryCode,String regionCode,String city,String district,String address,
            double latitude,double longitude,AssetStatus status,String correlationId,String actorId){
        if(source==null||source.isBlank()||externalId==null||externalId.isBlank())throw new CatalogException("INVALID_EXTERNAL_ASSET","source and externalId are required");
        CatalogAsset asset=repository.findBySourceAndExternalId(source,externalId).orElse(null);
        String desiredSlug=slug(requestedSlug,name);
        if(asset==null){
            if(repository.existsBySlugAndIdNot(desiredSlug,new UUID(0,0))) desiredSlug=desiredSlug+"-"+shortSuffix(externalId);
            asset=CatalogAsset.create(type,ownerId,source,externalId,name,desiredSlug,
                    description,categoryCode,regionCode,city,district,address,new GeoPoint(latitude,longitude));
        }else{
            asset.update(name,desiredSlug,description,categoryCode,regionCode,city,district,address,
                    new GeoPoint(latitude,longitude));
        }
        asset.synchronizeStatus(status);
        CatalogAsset saved=repository.save(asset);
        outbox.append("catalog.asset.synchronized",saved,correlationId,actorId);
        return saved;
    }
    @Transactional(readOnly=true) public CatalogAsset get(UUID id){CatalogAsset asset=getRequired(id);if(asset.getStatus()!=AssetStatus.PUBLISHED)throw new CatalogException("CATALOG_ASSET_NOT_FOUND","Catalog asset not found");return asset;}
    @Transactional(readOnly=true) public CatalogAsset getForManagement(UUID id){return getRequired(id);}
    @Transactional(readOnly=true) public CatalogAsset getBySlug(String slug){
        return repository.findBySlug(slug).filter(a->a.getStatus()==AssetStatus.PUBLISHED)
                .orElseThrow(()->new CatalogException("CATALOG_ASSET_NOT_FOUND","Catalog asset not found"));
    }
    @Transactional(readOnly=true) public List<CatalogAsset> search(AssetType type,String region,String category,String q,int limit){
        return repository.search(AssetStatus.PUBLISHED,type,region,category,q,Math.max(1,Math.min(limit,100)));
    }
    @Transactional(readOnly=true) public List<CatalogAsset> nearby(double lat,double lng,double radiusKm,
            AssetType type,String category,int limit){
        if(radiusKm<=0||radiusKm>100)throw new CatalogException("INVALID_RADIUS","radiusKm must be between 0 and 100");
        return repository.findNearby(lat,lng,radiusKm*1000,type,category,Math.max(1,Math.min(limit,100)));
    }
    private CatalogAsset getRequired(UUID id){return repository.findById(id)
            .orElseThrow(()->new CatalogException("CATALOG_ASSET_NOT_FOUND","Catalog asset not found"));}
    private void ensureSlug(String slug,UUID id){
        if(repository.existsBySlugAndIdNot(slug,id==null?new UUID(0,0):id))
            throw new CatalogException("CATALOG_SLUG_EXISTS","Catalog slug already exists");
    }
    private String slug(String requested,String name){
        String value=requested==null||requested.isBlank()?name:requested;
        String normalized=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");
        if(normalized.isBlank())throw new CatalogException("INVALID_SLUG","A valid slug is required");
        return normalized;
    }
    private String shortSuffix(String value){String normalized=value.replaceAll("[^A-Za-z0-9]","").toLowerCase(Locale.ROOT);return normalized.substring(0,Math.min(8,normalized.length()));}
}
