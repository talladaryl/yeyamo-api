package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringCheckInRepository extends JpaRepository<CheckInEntity,UUID>{List<CheckInEntity>findByUserIdOrderByOccurredAtDesc(String user,Pageable page);}
