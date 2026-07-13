package com.yeyamo_mobile.api.catalog_service.application;
public class CatalogException extends RuntimeException {
    private final String code;
    public CatalogException(String code,String message){super(message);this.code=code;}
    public String getCode(){return code;}
}
