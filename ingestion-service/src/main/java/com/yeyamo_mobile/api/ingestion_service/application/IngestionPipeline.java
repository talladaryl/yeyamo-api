package com.yeyamo_mobile.api.ingestion_service.application;
import java.util.List;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.ingestion_service.application.source.*;
import com.yeyamo_mobile.api.ingestion_service.domain.model.ImportJob;
@Component
public class IngestionPipeline {
    private final List<IngestionSourceStrategy> strategies;
    public IngestionPipeline(List<IngestionSourceStrategy> strategies){this.strategies=strategies;}
    public List<RawRecord> extract(ImportJob job){return strategies.stream().filter(s->s.supports(job.getSourceType())).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Unsupported source type")).extract(job);}
}
