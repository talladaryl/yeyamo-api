package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface InvoiceRepository extends JpaRepository<Invoice,UUID>{Optional<Invoice>findByOrderId(UUID o);}
