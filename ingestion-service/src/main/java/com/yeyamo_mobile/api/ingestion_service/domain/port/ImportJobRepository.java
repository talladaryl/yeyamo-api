package com.yeyamo_mobile.api.ingestion_service.domain.port;
import java.util.*;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
public interface ImportJobRepository {
    ImportJob save(ImportJob job);
    Optional<ImportJob> findById(UUID id);
    Optional<ImportJob> findByIdempotencyKey(String key);
    List<ImportJob> findPending(int limit);
}
