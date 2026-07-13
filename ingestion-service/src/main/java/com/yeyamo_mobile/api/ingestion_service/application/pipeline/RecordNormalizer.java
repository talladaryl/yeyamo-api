package com.yeyamo_mobile.api.ingestion_service.application.pipeline;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.ingestion_service.application.source.RawRecord;
import com.yeyamo_mobile.api.ingestion_service.domain.model.NormalizedCatalogRecord;
@Component
public class RecordNormalizer {
    public NormalizedCatalogRecord normalize(String source,RawRecord raw){
        Map<String,String> v=raw.values();String name=text(v,"name","nom","title");
        String external=text(v,"external_id","externalid","id","source_id");
        String type=upper(text(v,"asset_type","type","kind"),"PLACE");
        String category=slug(text(v,"category_code","category","categorie"));
        String region=upper(text(v,"region_code","region","regioncode"),null);
        Double lat=number(text(v,"latitude","lat"));Double lng=number(text(v,"longitude","lng","lon"));
        String fingerprint=Fingerprint.of(source,external,type,name,lat,lng);
        return new NormalizedCatalogRecord(source,external,type,trim(name),trim(text(v,"description","desc")),
                category,region,trim(text(v,"city","ville")),trim(text(v,"district","quartier")),
                trim(text(v,"address","adresse")),lat,lng,fingerprint,List.of());
    }
    private String text(Map<String,String> v,String...keys){for(String k:keys){String x=v.get(k);if(x!=null&&!x.isBlank())return x;}return null;}
    private Double number(String v){if(v==null)return null;try{return Double.valueOf(v.replace(',','.'));}catch(NumberFormatException e){return null;}}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private String upper(String v,String d){String x=trim(v);return x==null?d:x.toUpperCase(Locale.ROOT);}
    private String slug(String v){String x=trim(v);if(x==null)return null;return Normalizer.normalize(x,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");}
}
