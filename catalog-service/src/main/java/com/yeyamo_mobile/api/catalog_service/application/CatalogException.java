package com.yeyamo_mobile.api.catalog_service.application;

import org.springframework.http.HttpStatus;

public class CatalogException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    
    public CatalogException(String code, String message) {
        super(message);
        this.code = code;
        this.status = HttpStatus.BAD_REQUEST;
    }
    
    public CatalogException(String code, String message, HttpStatus status) {
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