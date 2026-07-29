package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="search_reindex_jobs")public class SearchReindexJobEntity{@Id public UUID id;public String status;@Column(name="requested_by",nullable=false)public String requestedBy;@Column(name="created_at",nullable=false)public Instant createdAt;@Column(name="started_at")public Instant startedAt;@Column(name="completed_at")public Instant completedAt;@Column(name="error_code")public String errorCode;}
