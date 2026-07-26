package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.*;import java.math.*;import java.util.*;
public interface LedgerRepository extends JpaRepository<LedgerEntry,UUID>{Optional<LedgerEntry>findByIdempotencyKey(String k);List<LedgerEntry>findByPartnerIdOrderByOccurredAtAsc(String p);@Query("select coalesce(sum(e.amount),0) from LedgerEntry e where e.partnerId=:p and e.currency=:c")BigDecimal balance(@Param("p")String p,@Param("c")String c);}
