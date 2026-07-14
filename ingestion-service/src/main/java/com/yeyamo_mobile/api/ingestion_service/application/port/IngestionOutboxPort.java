package com.yeyamo_mobile.api.ingestion_service.application.port;
import java.util.UUID;
import com.yeyamo_mobile.api.ingestion_service.domain.model.NormalizedCatalogRecord;
public interface IngestionOutboxPort { void append(UUID jobId,NormalizedCatalogRecord record); }
