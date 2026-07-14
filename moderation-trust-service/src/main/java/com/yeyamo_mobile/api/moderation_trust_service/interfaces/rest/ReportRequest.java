package com.yeyamo_mobile.api.moderation_trust_service.interfaces.rest;import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;import jakarta.validation.constraints.*;
public record ReportRequest(@NotNull TargetType targetType,@NotBlank@Size(max=120)String targetId,@Size(max=120)String targetOwnerId,@NotNull ReportReason reason,@Size(max=2000)String details){}
