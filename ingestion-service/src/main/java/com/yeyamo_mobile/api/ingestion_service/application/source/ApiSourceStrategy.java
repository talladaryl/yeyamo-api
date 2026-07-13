package com.yeyamo_mobile.api.ingestion_service.application.source;
import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
@Component
public class ApiSourceStrategy implements IngestionSourceStrategy {
    private final RestClient client;private final JsonSourceStrategy json;private final Set<String> allowedHosts;
    public ApiSourceStrategy(JsonSourceStrategy json,
            @Value("${ingestion.api.allowed-hosts:}")String hosts){
        client=RestClient.create();this.json=json;allowedHosts=new HashSet<>();
        Arrays.stream(hosts.split(",")).map(String::trim).filter(s->!s.isBlank()).map(s->s.toLowerCase(Locale.ROOT)).forEach(allowedHosts::add);
    }
    public boolean supports(SourceType t){return t==SourceType.API;}
    public List<RawRecord> extract(ImportJob job){
        URI uri=URI.create(job.getSourceReference());
        if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||!allowedHosts.contains(uri.getHost().toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("API host is not allowed");
        String body=client.get().uri(uri).retrieve().body(String.class);
        if(body==null||body.isBlank())throw new IllegalArgumentException("API returned an empty payload");
        return json.parse(body);
    }
}
