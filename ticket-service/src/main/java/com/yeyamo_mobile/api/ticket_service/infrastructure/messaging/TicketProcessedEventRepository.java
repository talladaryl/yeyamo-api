package com.yeyamo_mobile.api.ticket_service.infrastructure.messaging;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface TicketProcessedEventRepository extends JpaRepository<TicketProcessedEvent,UUID>{}
