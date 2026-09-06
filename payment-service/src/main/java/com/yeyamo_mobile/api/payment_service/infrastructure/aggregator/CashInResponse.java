package com.yeyamo_mobile.api.payment_service.infrastructure.aggregator;

public record CashInResponse(
    String reference,
    String transactionId,
    String status
) {}
