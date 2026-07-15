package com.yeyamo_mobile.api.ingestion_service.application.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record PayloadLimits(int maximumRecords, int maximumFields, int maximumValueLength) {
    public PayloadLimits(
            @Value("${ingestion.payload.max-records:10000}") int maximumRecords,
            @Value("${ingestion.payload.max-fields:100}") int maximumFields,
            @Value("${ingestion.payload.max-value-length:10000}") int maximumValueLength) {
        this.maximumRecords = positive(maximumRecords, "maximumRecords");
        this.maximumFields = positive(maximumFields, "maximumFields");
        this.maximumValueLength = positive(maximumValueLength, "maximumValueLength");
    }

    public static PayloadLimits defaults() {
        return new PayloadLimits(10_000, 100, 10_000);
    }

    private static int positive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
