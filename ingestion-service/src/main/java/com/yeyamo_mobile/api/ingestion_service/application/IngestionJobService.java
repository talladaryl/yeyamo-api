package com.yeyamo_mobile.api.ingestion_service.application;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
import com.yeyamo_mobile.api.ingestion_service.domain.port.ImportJobRepository;
@Service
public class IngestionJobService {
    private final ImportJobRepository repository;
    public IngestionJobService(ImportJobRepository repository){this.repository=repository;}
    @Transactional public ImportJob submit(String key,SourceType type,String reference,String payload){
        return repository.findByIdempotencyKey(key).orElseGet(()->repository.save(ImportJob.create(key,type,reference,payload)));
    }
    @Transactional(readOnly=true) public ImportJob get(UUID id){return repository.findById(id)
            .orElseThrow(()->new IngestionException("IMPORT_NOT_FOUND","Import job not found"));}
}
