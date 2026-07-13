package com.yeyamo_mobile.api.ingestion_service.application;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.ingestion_service.application.pipeline.*;
import com.yeyamo_mobile.api.ingestion_service.application.port.*;
import com.yeyamo_mobile.api.ingestion_service.application.source.RawRecord;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
import com.yeyamo_mobile.api.ingestion_service.domain.port.ImportJobRepository;
@Service
public class IngestionPersistenceService {
    private final ImportJobRepository jobs;private final IngestionRecordPort records;private final IngestionOutboxPort outbox;
    private final RecordNormalizer normalizer;private final RecordValidator validator;
    public IngestionPersistenceService(ImportJobRepository j,IngestionRecordPort r,IngestionOutboxPort o,RecordNormalizer n,RecordValidator v){
        jobs=j;records=r;outbox=o;normalizer=n;validator=v;
    }
    @Transactional public void start(UUID id){ImportJob j=required(id);j.start();jobs.save(j);}
    @Transactional public void persist(UUID id,List<RawRecord> raw){
        ImportJob job=required(id);int accepted=0,rejected=0,duplicates=0;
        String source=job.getSourceReference()==null?job.getSourceType().name().toLowerCase(Locale.ROOT):job.getSourceReference();
        for(RawRecord item:raw){
            NormalizedCatalogRecord record=validator.validate(normalizer.normalize(source,item));
            if(!record.valid()){records.save(id,record,RecordStatus.REJECTED);rejected++;continue;}
            if(records.existsByFingerprint(record.fingerprint())){records.save(id,record,RecordStatus.DUPLICATE);duplicates++;continue;}
            records.save(id,record,RecordStatus.ACCEPTED);outbox.append(id,record);accepted++;
        }
        job.complete(raw.size(),accepted,rejected,duplicates);jobs.save(job);
    }
    @Transactional public void fail(UUID id,Exception ex){ImportJob job=required(id);job.fail(ex.getMessage());jobs.save(job);}
    private ImportJob required(UUID id){return jobs.findById(id).orElseThrow(()->new IngestionException("IMPORT_NOT_FOUND","Import job not found"));}
}
