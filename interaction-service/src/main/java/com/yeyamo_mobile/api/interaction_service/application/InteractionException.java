package com.yeyamo_mobile.api.interaction_service.application;
public class InteractionException extends RuntimeException{private final String code;public InteractionException(String c,String m){super(m);code=c;}public String getCode(){return code;}}
