package com.yeyamo_mobile.api.auth_service.dto;

public record AdminPlatformUserDetail(
        AdminPlatformUserSummary user,
        long activeSessionCount,
        boolean profileProjectionAvailable,
        String partnerAccountStatus,
        Integer trustScore,
        Long reportCount,
        Long bookingCount) {
}
