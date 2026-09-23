package com.yeyamo_mobile.api.place_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeyamo_mobile.api.place_service.models.PlaceSuggestion;

import jakarta.persistence.LockModeType;

public interface PlaceSuggestionRepository extends JpaRepository<PlaceSuggestion, UUID> {
    @Override
    @EntityGraph(attributePaths = "media")
    Page<PlaceSuggestion> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "media")
    Page<PlaceSuggestion> findBySubmitterUserIdOrderByCreatedAtDesc(String submitterUserId, Pageable pageable);
    @EntityGraph(attributePaths = "media")
    Page<PlaceSuggestion> findByStatusOrderByCreatedAtDesc(PlaceSuggestion.Status status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PlaceSuggestion s where s.id = :id")
    Optional<PlaceSuggestion> findLockedById(@Param("id") UUID id);

    @Query("""
            select s from PlaceSuggestion s
            where s.status = :status
              and s.countryCode = :countryCode
              and (s.normalizedName = :name or s.normalizedAddress = :address)
            """)
    List<PlaceSuggestion> findDuplicateCandidates(@Param("status") PlaceSuggestion.Status status,
            @Param("countryCode") String countryCode, @Param("name") String name, @Param("address") String address);
}
