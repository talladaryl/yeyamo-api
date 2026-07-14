package com.yeyamo_mobile.api.user_service.application.exception;

import org.springframework.http.HttpStatus;

public class UserProfileException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public UserProfileException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
