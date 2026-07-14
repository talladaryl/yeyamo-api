package com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;import com.yeyamo_mobile.api.ingestion_service.domain.model.RecordStatus;
@Entity @Table(name="ingestion_records")
public class IngestionRecordEntity{
 @Id private UUID id;@Column(name="job_id",nullable=false)private UUID jobId;@Column(nullable=false,length=1000)private String source;
 @Column(name="external_id",length=200)private String externalId;@Column(nullable=false,length=64)private String fingerprint;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private RecordStatus status;@Column(nullable=false,columnDefinition="TEXT")private String payload;
 @Column(columnDefinition="TEXT")private String errors;@Column(name="created_at",nullable=false)private Instant createdAt;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public UUID getJobId(){return jobId;}public void setJobId(UUID v){jobId=v;}public String getSource(){return source;}public void setSource(String v){source=v;}
 public String getExternalId(){return externalId;}public void setExternalId(String v){externalId=v;}public String getFingerprint(){return fingerprint;}public void setFingerprint(String v){fingerprint=v;}
 public RecordStatus getStatus(){return status;}public void setStatus(RecordStatus v){status=v;}public String getPayload(){return payload;}public void setPayload(String v){payload=v;}
 public String getErrors(){return errors;}public void setErrors(String v){errors=v;}public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}
}
