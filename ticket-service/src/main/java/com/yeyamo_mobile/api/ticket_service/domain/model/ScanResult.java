package com.yeyamo_mobile.api.ticket_service.domain.model;

public enum ScanResult {
    VALID,
    ALREADY_USED,
    INVALID,
    EXPIRED,
    CANCELLED,
    REFUNDED,
    WRONG_EVENT,
    WRONG_GATE,
    NOT_YET_VALID,
    ACCESS_DENIED
}
