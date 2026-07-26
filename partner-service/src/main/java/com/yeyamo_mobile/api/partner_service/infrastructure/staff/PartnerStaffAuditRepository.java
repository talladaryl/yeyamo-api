package com.yeyamo_mobile.api.partner_service.infrastructure.staff;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface PartnerStaffAuditRepository extends JpaRepository<PartnerStaffAuditEntity,UUID>{List<PartnerStaffAuditEntity>findByPartnerIdOrderByOccurredAtDesc(UUID p);}
