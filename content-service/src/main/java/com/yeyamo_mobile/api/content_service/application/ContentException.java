package com.yeyamo_mobile.api.content_service.application;
public class ContentException extends RuntimeException{private final String code;public ContentException(String c,String m){super(m);code=c;}public String getCode(){return code;}}
