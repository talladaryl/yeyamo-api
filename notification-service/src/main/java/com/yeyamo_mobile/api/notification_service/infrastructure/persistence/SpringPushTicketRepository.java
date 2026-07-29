package com.yeyamo_mobile.api.notification_service.infrastructure.persistence;
import java.time.Instant;import java.util.*;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringPushTicketRepository extends JpaRepository<PushTicketEntity,UUID>{List<PushTicketEntity>findByStatusAndReceiptCheckedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(String status,Instant before,Pageable page);}
