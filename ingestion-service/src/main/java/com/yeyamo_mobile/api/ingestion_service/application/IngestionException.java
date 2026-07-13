package com.yeyamo_mobile.api.ingestion_service.application;
public class IngestionException extends RuntimeException{
    private final String code;public IngestionException(String c,String m){super(m);code=c;}public String getCode(){return code;}
}
