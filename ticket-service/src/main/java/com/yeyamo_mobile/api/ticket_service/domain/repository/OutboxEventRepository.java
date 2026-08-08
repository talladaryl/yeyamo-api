package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.published = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.aggregateType = :aggregateType " +
           "AND e.aggregateId = :aggregateId ORDER BY e.createdAt ASC")
    List<OutboxEvent> findByAggregate(String aggregateType, String aggregateId);
    
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.published = true AND e.publishedAt < :before")
    int deletePublishedEventsBefore(Instant before);
}
