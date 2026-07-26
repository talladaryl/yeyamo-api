package com.yeyamo_mobile.api.commerce_service.messaging;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface CommerceOutboxRepository extends JpaRepository<CommerceOutbox,UUID>{List<CommerceOutbox>findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
