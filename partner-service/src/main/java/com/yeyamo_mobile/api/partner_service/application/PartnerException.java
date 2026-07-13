package com.yeyamo_mobile.api.partner_service.application;
import org.springframework.http.HttpStatus;
public class PartnerException extends RuntimeException{private final String code;private final HttpStatus status;public PartnerException(String c,String m,HttpStatus s){super(m);code=c;status=s;}public String getCode(){return code;}public HttpStatus getStatus(){return status;}}
