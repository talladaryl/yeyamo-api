package com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import org.springframework.stereotype.Component;import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ingestion_service.application.port.IngestionRecordPort;import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
@Component
public class JpaIngestionRecordAdapter implements IngestionRecordPort{
 private final SpringIngestionRecordRepository repo;private final ObjectMapper mapper;public JpaIngestionRecordAdapter(SpringIngestionRecordRepository r,ObjectMapper m){repo=r;mapper=m;}
 public boolean existsByFingerprint(String f){return repo.existsByFingerprintAndStatus(f,RecordStatus.ACCEPTED);}
 public void save(UUID jobId,NormalizedCatalogRecord record,RecordStatus status){try{IngestionRecordEntity e=new IngestionRecordEntity();e.setId(UUID.randomUUID());e.setJobId(jobId);e.setSource(record.source());
  e.setExternalId(record.externalId());e.setFingerprint(record.fingerprint());e.setStatus(status);
  e.setPayload(mapper.writeValueAsString(record));e.setErrors(record.errors().isEmpty()?null:mapper.writeValueAsString(record.errors()));e.setCreatedAt(Instant.now());repo.save(e);
 }catch(Exception ex){throw new IllegalStateException("Cannot persist ingestion record",ex);}}
}
