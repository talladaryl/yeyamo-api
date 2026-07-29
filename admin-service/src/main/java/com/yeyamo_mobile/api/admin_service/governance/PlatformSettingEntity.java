package com.yeyamo_mobile.api.admin_service.governance;
import java.time.Instant;import jakarta.persistence.*;
@Entity@Table(name="platform_settings")public class PlatformSettingEntity{@Id@Column(name="setting_key")public String key;@Column(name="setting_value",nullable=false,columnDefinition="TEXT")public String value;@Column(name="value_type",nullable=false)public String type;@Column(nullable=false)public String description;public long version;@Column(name="updated_by",nullable=false)public String updatedBy;@Column(name="updated_at",nullable=false)public Instant updatedAt;}
