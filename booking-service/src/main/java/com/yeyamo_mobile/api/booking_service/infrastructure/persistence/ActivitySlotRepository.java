package com.yeyamo_mobile.api.booking_service.infrastructure.persistence;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ActivitySlotRepository extends JpaRepository<ActivitySlotEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ActivitySlotEntity s where s.id=:id")
    Optional<ActivitySlotEntity> findLocked(@Param("id") UUID id);

    List<ActivitySlotEntity> findByActivityIdAndStartsAtAfterOrderByStartsAtAsc(String activity, Instant after);

    Page<ActivitySlotEntity> findByPlaceIdAndStartsAtAfterOrderByStartsAtAsc(UUID placeId, Instant after, Pageable pageable);

    Page<ActivitySlotEntity> findByPlaceIdOrderByStartsAtAsc(UUID placeId, Pageable pageable);

    Page<ActivitySlotEntity> findByStartsAtAfterOrderByStartsAtAsc(Instant after, Pageable pageable);
}
