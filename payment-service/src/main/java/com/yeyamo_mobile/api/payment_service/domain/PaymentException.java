package com.yeyamo_mobile.api.payment_service.domain;
public class PaymentException extends RuntimeException { private final String code; public PaymentException(String code,String message){super(message);this.code=code;}public String getCode(){return code;} }
