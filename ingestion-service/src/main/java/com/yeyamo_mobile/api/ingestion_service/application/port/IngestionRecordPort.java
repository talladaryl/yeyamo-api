package com.yeyamo_mobile.api.ingestion_service.application.port;
import java.util.UUID;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
public interface IngestionRecordPort {
    boolean existsByFingerprint(String fingerprint);
    void save(UUID jobId,NormalizedCatalogRecord record,RecordStatus status);
}
