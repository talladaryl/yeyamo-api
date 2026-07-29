package com.yeyamo_mobile.api.analytics_service.business;
import java.time.Instant;import java.util.UUID;import org.springframework.scheduling.annotation.Async;import org.springframework.stereotype.Service;
@Service public class AnalyticsRebuildWorker{
 private final AnalyticsRebuildJobRepository jobs;private final BusinessAnalyticsService analytics;
 public AnalyticsRebuildWorker(AnalyticsRebuildJobRepository jobs,BusinessAnalyticsService analytics){this.jobs=jobs;this.analytics=analytics;}
 @Async public void execute(UUID id){AnalyticsRebuildJob job=jobs.findById(id).orElseThrow();job.status="RUNNING";job.startedAt=Instant.now();jobs.save(job);try{job.eventsReplayed=analytics.rebuild();job.status="COMPLETED";job.completedAt=Instant.now();}catch(RuntimeException exception){job.status="FAILED";job.failureReason=exception.getClass().getSimpleName();job.completedAt=Instant.now();}jobs.save(job);}
}
