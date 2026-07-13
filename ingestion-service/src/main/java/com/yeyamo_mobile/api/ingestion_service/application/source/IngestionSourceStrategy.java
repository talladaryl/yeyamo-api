package com.yeyamo_mobile.api.ingestion_service.application.source;
import java.util.List;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
public interface IngestionSourceStrategy {
    boolean supports(SourceType type);
    List<RawRecord> extract(ImportJob job);
}
