package com.yeyamo_mobile.api.catalog_service.application.port;

import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;

public interface CatalogOutboxPort {
    void append(String eventType, CatalogAsset asset, String correlationId, String actorId);
}
