package com.yeyamo_mobile.api.admin_service.governance;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="feature_flag_history")public class FeatureFlagHistoryEntity{@Id public UUID id;@Column(name="flag_key")public String key;public String description;public boolean enabled;public String environment;@Column(name="rollout_percentage")public Integer rolloutPercentage;public long version;@Column(name="updated_by")public String updatedBy;@Column(name="updated_at")public Instant updatedAt;@Column(name="change_reason")public String reason;}
