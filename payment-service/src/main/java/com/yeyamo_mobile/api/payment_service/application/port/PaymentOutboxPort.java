package com.yeyamo_mobile.api.payment_service.application.port;
import java.util.Map;
public interface PaymentOutboxPort{void append(String eventType,String aggregateId,String correlationId,Map<String,Object>payload);}
