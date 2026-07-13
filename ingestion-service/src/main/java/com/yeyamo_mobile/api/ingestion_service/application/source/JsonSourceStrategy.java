package com.yeyamo_mobile.api.ingestion_service.application.source;
import java.util.*;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.*;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
@Component
public class JsonSourceStrategy implements IngestionSourceStrategy {
    private final ObjectMapper mapper;
    public JsonSourceStrategy(ObjectMapper mapper){this.mapper=mapper;}
    public boolean supports(SourceType t){return t==SourceType.JSON;}
    public List<RawRecord> extract(ImportJob job){return parse(job.getInputPayload());}
    public List<RawRecord> parse(String payload){
        try{
            JsonNode root=mapper.readTree(payload);JsonNode records=root.isArray()?root:root.path("data");
            if(!records.isArray())throw new IllegalArgumentException("JSON must be an array or contain a data array");
            List<RawRecord> result=new ArrayList<>();int row=1;
            for(JsonNode node:records){
                if(!node.isObject())throw new IllegalArgumentException("Each JSON record must be an object");
                Map<String,String> values=new LinkedHashMap<>();
                node.fields().forEachRemaining(e->values.put(e.getKey().toLowerCase(Locale.ROOT),e.getValue().isNull()?null:e.getValue().asText()));
                result.add(new RawRecord(row++,values));
            }return result;
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalArgumentException("Malformed JSON payload",ex);}
    }
}
