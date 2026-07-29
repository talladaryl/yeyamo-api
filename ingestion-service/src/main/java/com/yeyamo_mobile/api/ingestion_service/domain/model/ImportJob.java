package com.yeyamo_mobile.api.ingestion_service.domain.model;
import java.time.Instant;
import java.util.UUID;
public class ImportJob {
    private UUID id; private String idempotencyKey; private SourceType sourceType;
    private String sourceReference; private String inputPayload; private JobStatus status;
    private int totalRecords; private int acceptedRecords; private int rejectedRecords;
    private int duplicateRecords; private String errorMessage;
    private Instant createdAt; private Instant startedAt; private Instant completedAt; private long version;
    public static ImportJob create(String key,SourceType type,String reference,String payload){
        if(key==null||key.isBlank())throw new IllegalArgumentException("Idempotency-Key is required");
        if(type==null)throw new IllegalArgumentException("sourceType is required");
        if(type!=SourceType.API&&(payload==null||payload.isBlank()))throw new IllegalArgumentException("payload is required");
        if(type==SourceType.API&&(reference==null||reference.isBlank()))throw new IllegalArgumentException("sourceReference URL is required");
        ImportJob j=new ImportJob();j.id=UUID.randomUUID();j.idempotencyKey=key.trim();
        j.sourceType=type;j.sourceReference=trim(reference);j.inputPayload=payload;
        j.status=JobStatus.PENDING;j.createdAt=Instant.now();return j;
    }
    public void start(){if(status!=JobStatus.PENDING)throw new IllegalStateException("Only pending jobs can start");status=JobStatus.PROCESSING;startedAt=Instant.now();}
    public void complete(int total,int accepted,int rejected,int duplicates){
        totalRecords=total;acceptedRecords=accepted;rejectedRecords=rejected;duplicateRecords=duplicates;
        status=rejected>0?JobStatus.PARTIAL:JobStatus.COMPLETED;completedAt=Instant.now();inputPayload=null;
    }
    public void fail(String message){status=JobStatus.FAILED;errorMessage=abbreviate(message);completedAt=Instant.now();}
    public void cancel(){if(status!=JobStatus.PENDING&&status!=JobStatus.PROCESSING)throw new IllegalStateException("Only pending or processing jobs can be cancelled");status=JobStatus.CANCELLED;completedAt=Instant.now();inputPayload=null;}
    public void retry(){if(status!=JobStatus.FAILED&&status!=JobStatus.PARTIAL&&status!=JobStatus.COMPLETED_WITH_ERRORS)throw new IllegalStateException("Only failed or partial jobs can be retried");if(inputPayload==null&&sourceType!=SourceType.API)throw new IllegalStateException("Import source is no longer available");status=JobStatus.PENDING;startedAt=null;completedAt=null;errorMessage=null;totalRecords=acceptedRecords=rejectedRecords=duplicateRecords=0;}
    private String abbreviate(String v){if(v==null)return "Unknown ingestion failure";return v.substring(0,Math.min(v.length(),2000));}
    private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    public SourceType getSourceType(){return sourceType;} public void setSourceType(SourceType v){sourceType=v;}
    public String getSourceReference(){return sourceReference;} public void setSourceReference(String v){sourceReference=v;}
    public String getInputPayload(){return inputPayload;} public void setInputPayload(String v){inputPayload=v;}
    public JobStatus getStatus(){return status;} public void setStatus(JobStatus v){status=v;}
    public int getTotalRecords(){return totalRecords;} public void setTotalRecords(int v){totalRecords=v;}
    public int getAcceptedRecords(){return acceptedRecords;} public void setAcceptedRecords(int v){acceptedRecords=v;}
    public int getRejectedRecords(){return rejectedRecords;} public void setRejectedRecords(int v){rejectedRecords=v;}
    public int getDuplicateRecords(){return duplicateRecords;} public void setDuplicateRecords(int v){duplicateRecords=v;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
    public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;}
}
