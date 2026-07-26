package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface OrderLineRepository extends JpaRepository<OrderLine,UUID>{List<OrderLine>findByOrderId(UUID id);}
