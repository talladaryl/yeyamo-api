package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.time.Instant;import jakarta.persistence.*;
@Entity @Table(name="search_zero_results")public class SearchZeroResultEntity{@Id@Column(name="query_hash")public String queryHash;@Column(name="normalized_query",nullable=false)public String normalizedQuery;@Column(name="region_code")public String regionCode;public long occurrences;@Column(name="first_seen_at",nullable=false)public Instant firstSeenAt;@Column(name="last_seen_at",nullable=false)public Instant lastSeenAt;}
