package com.yeyamo_mobile.api.analytics_service.business;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="analytics_rebuild_jobs")public class AnalyticsRebuildJob{
 @Id public UUID id;public String status;@Column(name="requested_by")public String requestedBy;@Column(name="created_at")public Instant createdAt;@Column(name="started_at")public Instant startedAt;@Column(name="completed_at")public Instant completedAt;@Column(name="events_replayed")public Long eventsReplayed;@Column(name="failure_reason")public String failureReason;
}
