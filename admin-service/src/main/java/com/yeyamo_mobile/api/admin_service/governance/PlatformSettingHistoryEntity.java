package com.yeyamo_mobile.api.admin_service.governance;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="platform_setting_history")public class PlatformSettingHistoryEntity{@Id public UUID id;@Column(name="setting_key")public String key;@Column(name="setting_value",columnDefinition="TEXT")public String value;@Column(name="value_type")public String type;public long version;@Column(name="updated_by")public String updatedBy;@Column(name="updated_at")public Instant updatedAt;@Column(name="change_reason")public String reason;}
