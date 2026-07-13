package com.yeyamo_mobile.api.ingestion_service.application;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.ingestion_service.domain.model.ImportJob;
import com.yeyamo_mobile.api.ingestion_service.domain.port.ImportJobRepository;
import org.springframework.dao.OptimisticLockingFailureException;
@Component
@ConditionalOnProperty(name="ingestion.worker.enabled",havingValue="true",matchIfMissing=true)
public class IngestionWorker {
    private final ImportJobRepository jobs;private final IngestionPipeline pipeline;private final IngestionPersistenceService persistence;
    public IngestionWorker(ImportJobRepository j,IngestionPipeline p,IngestionPersistenceService s){jobs=j;pipeline=p;persistence=s;}
    @Scheduled(fixedDelayString="${ingestion.worker.delay-ms:1000}")
    public void runPending(){
        for(ImportJob job:jobs.findPending(10)){
            try{persistence.start(job.getId());}
            catch(OptimisticLockingFailureException|IllegalStateException concurrentClaim){continue;}
            try{
                ImportJob processing=jobs.findById(job.getId()).orElseThrow();
                persistence.persist(job.getId(),pipeline.extract(processing));
            }catch(Exception ex){persistence.fail(job.getId(),ex);}
        }
    }
}
