package com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.PageRequest;import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;import com.yeyamo_mobile.api.ingestion_service.domain.port.ImportJobRepository;
@Component
public class JpaImportJobRepositoryAdapter implements ImportJobRepository{
 private final SpringImportJobRepository repo;public JpaImportJobRepositoryAdapter(SpringImportJobRepository r){repo=r;}
 public ImportJob save(ImportJob j){return domain(repo.save(entity(j)));}
 public Optional<ImportJob> findById(UUID id){return repo.findById(id).map(this::domain);}
 public Optional<ImportJob> findByIdempotencyKey(String k){return repo.findByIdempotencyKey(k).map(this::domain);}
 public List<ImportJob> findPending(int limit){return repo.findByStatusOrderByCreatedAtAsc(JobStatus.PENDING,PageRequest.of(0,limit)).stream().map(this::domain).toList();}
 private ImportJobEntity entity(ImportJob j){ImportJobEntity e=new ImportJobEntity();e.setId(j.getId());e.setIdempotencyKey(j.getIdempotencyKey());e.setSourceType(j.getSourceType());
  e.setSourceReference(j.getSourceReference());e.setInputPayload(j.getInputPayload());e.setStatus(j.getStatus());e.setTotalRecords(j.getTotalRecords());e.setAcceptedRecords(j.getAcceptedRecords());
  e.setRejectedRecords(j.getRejectedRecords());e.setDuplicateRecords(j.getDuplicateRecords());e.setErrorMessage(j.getErrorMessage());e.setCreatedAt(j.getCreatedAt());e.setStartedAt(j.getStartedAt());
  e.setCompletedAt(j.getCompletedAt());e.setVersion(j.getVersion());return e;}
 public ImportJob toDomain(ImportJobEntity e){return domain(e);}private ImportJob domain(ImportJobEntity e){ImportJob j=new ImportJob();j.setId(e.getId());j.setIdempotencyKey(e.getIdempotencyKey());j.setSourceType(e.getSourceType());
  j.setSourceReference(e.getSourceReference());j.setInputPayload(e.getInputPayload());j.setStatus(e.getStatus());j.setTotalRecords(e.getTotalRecords());j.setAcceptedRecords(e.getAcceptedRecords());
  j.setRejectedRecords(e.getRejectedRecords());j.setDuplicateRecords(e.getDuplicateRecords());j.setErrorMessage(e.getErrorMessage());j.setCreatedAt(e.getCreatedAt());j.setStartedAt(e.getStartedAt());
  j.setCompletedAt(e.getCompletedAt());j.setVersion(e.getVersion());return j;}
}
