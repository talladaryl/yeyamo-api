package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="search_synonyms")public class SearchSynonymEntity{@Id public UUID id;@Column(name="terms_json",nullable=false,columnDefinition="TEXT")public String termsJson;public boolean active;@Column(name="created_by",nullable=false)public String createdBy;@Column(name="created_at",nullable=false)public Instant createdAt;@Column(name="updated_at",nullable=false)public Instant updatedAt;}
