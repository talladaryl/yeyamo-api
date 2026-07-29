package com.yeyamo_mobile.api.analytics_service.business;
import java.time.Instant;import java.util.UUID;import org.springframework.scheduling.annotation.Async;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service public class AnalyticsRebuildService{
 private final AnalyticsRebuildJobRepository jobs;private final AnalyticsRebuildWorker worker;
 public AnalyticsRebuildService(AnalyticsRebuildJobRepository jobs,AnalyticsRebuildWorker worker){this.jobs=jobs;this.worker=worker;}
 public AnalyticsRebuildJob start(String actor){AnalyticsRebuildJob job=new AnalyticsRebuildJob();job.id=UUID.randomUUID();job.status="PENDING";job.requestedBy=actor;job.createdAt=Instant.now();jobs.save(job);worker.execute(job.id);return job;}
 @Transactional(readOnly=true)public AnalyticsRebuildJob get(UUID id){return jobs.findById(id).orElseThrow();}
}
