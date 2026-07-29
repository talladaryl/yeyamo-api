package com.yeyamo_mobile.api.admin_service.governance;
import java.time.Instant;import jakarta.persistence.*;
@Entity@Table(name="feature_flags")public class FeatureFlagEntity{@Id@Column(name="flag_key")public String key;public String description;public boolean enabled;public String environment;@Column(name="rollout_percentage")public Integer rolloutPercentage;public long version;@Column(name="updated_by")public String updatedBy;@Column(name="updated_at")public Instant updatedAt;}
