package com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
@Entity @Table(name="ingestion_jobs")
public class ImportJobEntity {
 @Id private UUID id;@Column(name="idempotency_key",nullable=false,unique=true,length=200)private String idempotencyKey;
 @Enumerated(EnumType.STRING)@Column(name="source_type",nullable=false,length=20)private SourceType sourceType;
 @Column(name="source_reference",length=1000)private String sourceReference;
 @Column(name="input_payload",columnDefinition="TEXT")private String inputPayload;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=40)private JobStatus status;
 @Column(name="total_records",nullable=false)private int totalRecords;@Column(name="accepted_records",nullable=false)private int acceptedRecords;
 @Column(name="rejected_records",nullable=false)private int rejectedRecords;@Column(name="duplicate_records",nullable=false)private int duplicateRecords;
 @Column(name="error_message",length=2000)private String errorMessage;@Column(name="created_at",nullable=false)private Instant createdAt;
 @Column(name="started_at")private Instant startedAt;@Column(name="completed_at")private Instant completedAt;@Version private long version;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public String getIdempotencyKey(){return idempotencyKey;}public void setIdempotencyKey(String v){idempotencyKey=v;}
 public SourceType getSourceType(){return sourceType;}public void setSourceType(SourceType v){sourceType=v;}public String getSourceReference(){return sourceReference;}public void setSourceReference(String v){sourceReference=v;}
 public String getInputPayload(){return inputPayload;}public void setInputPayload(String v){inputPayload=v;}public JobStatus getStatus(){return status;}public void setStatus(JobStatus v){status=v;}
 public int getTotalRecords(){return totalRecords;}public void setTotalRecords(int v){totalRecords=v;}public int getAcceptedRecords(){return acceptedRecords;}public void setAcceptedRecords(int v){acceptedRecords=v;}
 public int getRejectedRecords(){return rejectedRecords;}public void setRejectedRecords(int v){rejectedRecords=v;}public int getDuplicateRecords(){return duplicateRecords;}public void setDuplicateRecords(int v){duplicateRecords=v;}
 public String getErrorMessage(){return errorMessage;}public void setErrorMessage(String v){errorMessage=v;}public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}
 public Instant getStartedAt(){return startedAt;}public void setStartedAt(Instant v){startedAt=v;}public Instant getCompletedAt(){return completedAt;}public void setCompletedAt(Instant v){completedAt=v;}
 public long getVersion(){return version;}public void setVersion(long v){version=v;}
}
