package com.yeyamo_mobile.api.admin_service.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorResponse> details,
        Instant timestamp,
        String correlationId
) {
    public static ErrorResponse of(String code, String message, List<FieldErrorResponse> details, String correlationId) {
        return new ErrorResponse(code, message, details, Instant.now(), correlationId);
    }
}
