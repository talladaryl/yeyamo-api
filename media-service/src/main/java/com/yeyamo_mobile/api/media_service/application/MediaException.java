package com.yeyamo_mobile.api.media_service.application;
public class MediaException extends RuntimeException{private final String code;public MediaException(String c,String m){super(m);code=c;}public String getCode(){return code;}}
