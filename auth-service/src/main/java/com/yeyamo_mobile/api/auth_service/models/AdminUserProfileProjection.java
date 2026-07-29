package com.yeyamo_mobile.api.auth_service.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_user_profile_projection")
@Getter
@Setter
public class AdminUserProfileProjection {
    @Id
    @Column(name = "auth_user_id")
    private Long authUserId;
    @Column(name = "profile_id")
    private UUID profileId;
    @Column(name = "display_name", length = 100)
    private String displayName;
    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;
    @Column(name = "region_id")
    private Long regionId;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
