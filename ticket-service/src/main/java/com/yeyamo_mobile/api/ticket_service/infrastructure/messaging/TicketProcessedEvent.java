package com.yeyamo_mobile.api.ticket_service.infrastructure.messaging;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="ticket_processed_events")public class TicketProcessedEvent{@Id public UUID eventId;@Column(name="event_type")public String eventType;@Column(name="processed_at")public Instant processedAt;protected TicketProcessedEvent(){}public TicketProcessedEvent(UUID i,String t){eventId=i;eventType=t;processedAt=Instant.now();}}
