package com.yeyamo_mobile.api.ingestion_service.domain.model;
import java.util.List;
public record NormalizedCatalogRecord(String source,String externalId,String assetType,String name,
        String description,String categoryCode,String regionCode,String city,String district,String address,
        Double latitude,Double longitude,String fingerprint,List<String> errors){
    public boolean valid(){return errors==null||errors.isEmpty();}
}
