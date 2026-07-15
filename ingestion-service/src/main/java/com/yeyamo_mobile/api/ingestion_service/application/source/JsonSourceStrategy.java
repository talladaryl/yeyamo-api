package com.yeyamo_mobile.api.ingestion_service.application.source;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.*;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
@Component
public class JsonSourceStrategy implements IngestionSourceStrategy {
    private final ObjectMapper mapper;
    private final PayloadLimits limits;
    public JsonSourceStrategy(ObjectMapper mapper){this(mapper,PayloadLimits.defaults());}
    @Autowired public JsonSourceStrategy(ObjectMapper mapper,PayloadLimits limits){this.mapper=mapper;this.limits=limits;}
    public boolean supports(SourceType t){return t==SourceType.JSON;}
    public List<RawRecord> extract(ImportJob job){return parse(job.getInputPayload());}
    public List<RawRecord> parse(String payload){
        try{
            JsonNode root=mapper.readTree(payload);JsonNode records=root.isArray()?root:root.path("data");
            if(!records.isArray())throw new IllegalArgumentException("JSON must be an array or contain a data array");
            if(records.size()>limits.maximumRecords())throw new IllegalArgumentException("JSON contains too many records");
            List<RawRecord> result=new ArrayList<>();int row=1;
            for(JsonNode node:records){
                if(!node.isObject())throw new IllegalArgumentException("Each JSON record must be an object");
                if(node.size()>limits.maximumFields())throw new IllegalArgumentException("JSON record contains too many fields");
                Map<String,String> values=new LinkedHashMap<>();
                node.fields().forEachRemaining(e->{if(e.getKey().length()>100||e.getValue().isContainerNode())throw new IllegalArgumentException("Nested or oversized JSON fields are not allowed");String value=e.getValue().isNull()?null:e.getValue().asText();if(value!=null&&value.length()>limits.maximumValueLength())throw new IllegalArgumentException("JSON field value is too long");values.put(e.getKey().toLowerCase(Locale.ROOT),value);});
                result.add(new RawRecord(row++,values));
            }return result;
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalArgumentException("Malformed JSON payload",ex);}
    }
}
