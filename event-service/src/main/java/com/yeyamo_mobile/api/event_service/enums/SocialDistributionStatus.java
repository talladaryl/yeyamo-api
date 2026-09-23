package com.yeyamo_mobile.api.event_service.enums;

/**
 * Observable state of one requested social-distribution target for an Event.
 */
public enum SocialDistributionStatus {
    NOT_REQUESTED,
    PENDING_MODERATION,
    PROCESSING,
    PUBLISHED,
    SKIPPED_NO_MEDIA,
    SKIPPED_PRIVATE_EVENT,
    FAILED
}
