package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import org.springframework.http.HttpStatus;

public class AdventurePlanException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public AdventurePlanException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
