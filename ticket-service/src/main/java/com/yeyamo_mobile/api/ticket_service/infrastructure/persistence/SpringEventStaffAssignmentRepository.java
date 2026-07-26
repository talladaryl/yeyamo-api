package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringEventStaffAssignmentRepository extends JpaRepository<EventStaffAssignmentEntity, UUID> {
    
    List<EventStaffAssignmentEntity> findByEventIdOrderByCreatedAtDesc(String eventId);
    
    Optional<EventStaffAssignmentEntity> findByEventIdAndUserId(String eventId, String userId);
    
    @Query("SELECT s FROM EventStaffAssignmentEntity s WHERE s.userId = :userId AND s.status = 'ACTIVE' ORDER BY s.validFrom DESC")
    List<EventStaffAssignmentEntity> findActiveAssignmentsByUser(@Param("userId") String userId);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM EventStaffAssignmentEntity s " +
           "WHERE s.userId = :userId AND s.eventId = :eventId AND s.status = 'ACTIVE' " +
           "AND :now BETWEEN s.validFrom AND s.validUntil")
    boolean existsActiveAssignment(
        @Param("userId") String userId,
        @Param("eventId") String eventId,
        @Param("now") Instant now
    );
    
    @Query("SELECT s FROM EventStaffAssignmentEntity s WHERE s.eventId = :eventId AND s.role = :role AND s.status = 'ACTIVE'")
    List<EventStaffAssignmentEntity> findByEventIdAndRole(@Param("eventId") String eventId, @Param("role") StaffRole role);
}
