package com.yeyamo_mobile.api.media_service.domain.model;

/**
 * Tracks whether consent has been obtained for media that contains a third-party person.
 */
public enum ConsentStatus {
    /** No third-party person is identifiable in this media. */
    NOT_REQUIRED,
    /** Consent has been provided and is on file. */
    OBTAINED,
    /** Consent has been explicitly refused. */
    REFUSED,
    /** Consent was not verified at upload time. */
    PENDING
}
