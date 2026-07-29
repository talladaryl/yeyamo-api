package com.yeyamo_mobile.api.admin_service.governance;
import java.time.Instant;import jakarta.validation.constraints.*;
public final class GovernanceDtos{private GovernanceDtos(){}
 public record SettingResponse(String key,String value,String type,String description,long version,String updatedBy,Instant updatedAt){}
 public record SettingUpdateRequest(@NotBlank@Size(max=500)String value,@NotBlank@Size(max=500)String reason,long expectedVersion){}
 public record FeatureFlagRequest(@NotBlank@Pattern(regexp="[a-z][a-z0-9._-]{2,99}")String key,@NotBlank@Size(max=500)String description,boolean enabled,@NotBlank@Pattern(regexp="development|staging|production")String environment,@Min(0)@Max(100)Integer rolloutPercentage,@Size(max=500)String reason,long expectedVersion){}
 public record FeatureFlagResponse(String key,String description,boolean enabled,String environment,Integer rolloutPercentage,long version,String updatedBy,Instant updatedAt){}
}
