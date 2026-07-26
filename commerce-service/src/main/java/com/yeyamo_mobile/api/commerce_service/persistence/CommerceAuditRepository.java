package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface CommerceAuditRepository extends JpaRepository<CommerceAudit,UUID>{List<CommerceAudit>findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String t,String id);}
