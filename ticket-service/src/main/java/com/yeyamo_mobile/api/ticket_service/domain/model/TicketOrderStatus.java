package com.yeyamo_mobile.api.ticket_service.domain.model;

public enum TicketOrderStatus {
    CREATED,
    AWAITING_PAYMENT,
    PAID,
    ISSUED,
    CANCELLED,
    EXPIRED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
