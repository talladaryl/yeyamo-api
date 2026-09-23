package com.yeyamo_mobile.api.moderation_trust_service.application;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;

public interface ReportedTargetResolver {
    ReportedTarget require(TargetType type, String targetId);
}
