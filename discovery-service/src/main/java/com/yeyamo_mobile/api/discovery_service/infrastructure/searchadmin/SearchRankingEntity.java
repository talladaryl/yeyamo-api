package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="search_ranking_versions")public class SearchRankingEntity{@Id public UUID id;@Column(nullable=false,unique=true)public int version;@Column(name="config_json",nullable=false,columnDefinition="TEXT")public String configJson;public boolean active;@Column(name="created_by",nullable=false)public String createdBy;@Column(name="created_at",nullable=false)public Instant createdAt;}
