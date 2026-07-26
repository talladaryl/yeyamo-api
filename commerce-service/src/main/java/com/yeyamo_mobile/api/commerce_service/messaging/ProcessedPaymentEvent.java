package com.yeyamo_mobile.api.commerce_service.messaging;
import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="commerce_processed_events")public class ProcessedPaymentEvent{@Id public String eventId;@Column(name="event_type")public String eventType;@Column(name="processed_at")public Instant processedAt;protected ProcessedPaymentEvent(){}public ProcessedPaymentEvent(String i,String t){eventId=i;eventType=t;processedAt=Instant.now();}}
