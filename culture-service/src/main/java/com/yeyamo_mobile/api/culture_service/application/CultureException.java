package com.yeyamo_mobile.api.culture_service.application;
import org.springframework.http.HttpStatus;
public class CultureException extends RuntimeException{private final String code;private final HttpStatus status;public CultureException(String code,String message,HttpStatus status){super(message);this.code=code;this.status=status;}public String getCode(){return code;}public HttpStatus getStatus(){return status;}}
