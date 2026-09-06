package com.yeyamo.security.hardening;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response DTO aligned with YeYamo microservices format.
 */
public record ErrorResponse(
        String code,
        String message,
        List<String> details,
        Instant timestamp,
        String correlationId
) {
    public static ErrorResponse of(String code, String message, String correlationId) {
        return new ErrorResponse(code, message, List.of(), Instant.now(), correlationId != null ? correlationId : "");
    }
}
