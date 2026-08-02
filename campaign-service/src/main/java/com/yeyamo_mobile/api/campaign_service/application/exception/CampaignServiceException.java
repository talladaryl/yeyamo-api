package com.yeyamo_mobile.api.campaign_service.application.exception;

import org.springframework.http.HttpStatus;

public class CampaignServiceException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public CampaignServiceException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
