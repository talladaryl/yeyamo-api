package com.yeyamo_mobile.api.ingestion_service.application.pipeline;
import java.util.*;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.ingestion_service.domain.model.NormalizedCatalogRecord;
@Component
public class RecordValidator {
    private static final Set<String> TYPES=Set.of("DESTINATION","PLACE","EXPERIENCE","EVENT");
    public NormalizedCatalogRecord validate(NormalizedCatalogRecord r){
        List<String> errors=new ArrayList<>();
        if(r.name()==null||r.name().isBlank())errors.add("name is required");
        if(r.assetType()==null||!TYPES.contains(r.assetType()))errors.add("assetType is invalid");
        if(r.latitude()==null||r.latitude()<-90||r.latitude()>90)errors.add("latitude is invalid");
        if(r.longitude()==null||r.longitude()<-180||r.longitude()>180)errors.add("longitude is invalid");
        return new NormalizedCatalogRecord(r.source(),r.externalId(),r.assetType(),r.name(),r.description(),
                r.categoryCode(),r.regionCode(),r.city(),r.district(),r.address(),r.latitude(),r.longitude(),
                r.fingerprint(),List.copyOf(errors));
    }
}
