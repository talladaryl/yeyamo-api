package com.yeyamo_mobile.api.notification_service.application.port;
import java.time.Instant;import java.util.*;import com.yeyamo_mobile.api.notification_service.domain.Delivery;
public interface DeliveryRepositoryPort{Delivery save(Delivery delivery);List<Delivery>findDue(Instant now,int limit);}
