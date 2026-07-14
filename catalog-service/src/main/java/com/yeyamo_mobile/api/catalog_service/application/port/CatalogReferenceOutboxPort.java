package com.yeyamo_mobile.api.catalog_service.application.port;

import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogReference;

public interface CatalogReferenceOutboxPort {
    void append(String eventType, CatalogReference reference, String correlationId, String actorId);
}
