package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.EventStaffAssignment;
import com.yeyamo_mobile.api.ticket_service.domain.model.StaffRole;
import com.yeyamo_mobile.api.ticket_service.domain.model.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventStaffAssignmentRepository extends JpaRepository<EventStaffAssignment, String> {
    
    List<EventStaffAssignment> findByEventId(String eventId);
    
    List<EventStaffAssignment> findByUserId(String userId);
    
    Optional<EventStaffAssignment> findByEventIdAndUserId(String eventId, String userId);
    
    @Query("SELECT s FROM EventStaffAssignment s WHERE s.eventId = :eventId " +
           "AND s.userId = :userId AND s.status = 'ACTIVE' " +
           "AND s.validFrom <= :now AND s.validUntil > :now")
    Optional<EventStaffAssignment> findActiveAssignment(String eventId, String userId, Instant now);
    
    @Query("SELECT s FROM EventStaffAssignment s WHERE s.eventId = :eventId " +
           "AND s.role = :role AND s.status = 'ACTIVE'")
    List<EventStaffAssignment> findByEventIdAndRole(String eventId, StaffRole role);
    
    @Query("SELECT COUNT(s) FROM EventStaffAssignment s WHERE s.eventId = :eventId " +
           "AND s.status = 'ACTIVE'")
    long countActiveStaffByEvent(String eventId);
    
    boolean existsByEventIdAndUserId(String eventId, String userId);
}
