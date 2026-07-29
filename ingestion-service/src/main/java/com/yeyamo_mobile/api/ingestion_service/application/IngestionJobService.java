package com.yeyamo_mobile.api.ingestion_service.application;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;import org.springframework.data.jpa.domain.Specification;import java.time.Instant;import com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence.*;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
import com.yeyamo_mobile.api.ingestion_service.domain.port.ImportJobRepository;
@Service
public class IngestionJobService {
    private final ImportJobRepository repository;private final SpringImportJobRepository jobs;private final SpringIngestionRecordRepository records;
    @org.springframework.beans.factory.annotation.Autowired public IngestionJobService(ImportJobRepository repository,SpringImportJobRepository jobs,SpringIngestionRecordRepository records){this.repository=repository;this.jobs=jobs;this.records=records;}
    IngestionJobService(ImportJobRepository repository){this.repository=repository;this.jobs=null;this.records=null;}
    @Transactional public ImportJob submit(String key,SourceType type,String reference,String payload){
        return repository.findByIdempotencyKey(key).orElseGet(()->repository.save(ImportJob.create(key,type,reference,payload)));
    }
    @Transactional(readOnly=true) public ImportJob get(UUID id){return repository.findById(id)
            .orElseThrow(()->new IngestionException("IMPORT_NOT_FOUND","Import job not found"));}
    @Transactional(readOnly=true)public Page<ImportJob>list(JobStatus status,SourceType source,Instant from,Instant to,Pageable pageable){Specification<ImportJobEntity>s=(r,q,c)->c.conjunction();if(status!=null)s=s.and((r,q,c)->c.equal(r.get("status"),status));if(source!=null)s=s.and((r,q,c)->c.equal(r.get("sourceType"),source));if(from!=null)s=s.and((r,q,c)->c.greaterThanOrEqualTo(r.get("createdAt"),from));if(to!=null)s=s.and((r,q,c)->c.lessThanOrEqualTo(r.get("createdAt"),to));JpaImportJobRepositoryAdapter mapper=(JpaImportJobRepositoryAdapter)repository;return jobs.findAll(s,pageable).map(mapper::toDomain);}
    @Transactional public ImportJob cancel(UUID id){ImportJob job=get(id);job.cancel();return repository.save(job);}
    @Transactional public ImportJob retry(UUID id){ImportJob job=get(id);job.retry();return repository.save(job);}
    @Transactional(readOnly=true)public java.util.List<ImportError>errors(UUID id){get(id);java.util.concurrent.atomic.AtomicLong row=new java.util.concurrent.atomic.AtomicLong(0);return records.findByJobIdAndStatusOrderByCreatedAtAsc(id,RecordStatus.REJECTED).stream().map(r->new ImportError(row.incrementAndGet(),null,"INVALID_RECORD",safe(r.getErrors()))).toList();}
    private String safe(String value){return value==null||value.isBlank()?"Record rejected":value.substring(0,Math.min(1000,value.length()));}
    public record ImportError(long rowNumber,String field,String code,String message){}
}
