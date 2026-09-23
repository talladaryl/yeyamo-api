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

    @Query("select s from ActivitySlotEntity s where s.activityId = :activity and s.startsAt > :after "
            + "and s.status = com.yeyamo_mobile.api.booking_service.domain.SlotStatus.OPEN "
            + "and s.reservedCount < s.capacity order by s.startsAt asc")
    List<ActivitySlotEntity> findBookableByActivityId(@Param("activity") String activity, @Param("after") Instant after);

    Page<ActivitySlotEntity> findByPlaceIdAndStartsAtAfterOrderByStartsAtAsc(UUID placeId, Instant after, Pageable pageable);

    Page<ActivitySlotEntity> findByPlaceIdOrderByStartsAtAsc(UUID placeId, Pageable pageable);

    Page<ActivitySlotEntity> findByStartsAtAfterOrderByStartsAtAsc(Instant after, Pageable pageable);

    @Query("select s from ActivitySlotEntity s where s.startsAt > :after "
            + "and s.status = com.yeyamo_mobile.api.booking_service.domain.SlotStatus.OPEN "
            + "and s.reservedCount < s.capacity order by s.startsAt asc")
    Page<ActivitySlotEntity> findBookableAfter(@Param("after") Instant after, Pageable pageable);

    @Query("select s from ActivitySlotEntity s where s.placeId = :placeId and s.startsAt > :after "
            + "and s.status = com.yeyamo_mobile.api.booking_service.domain.SlotStatus.OPEN "
            + "and s.reservedCount < s.capacity order by s.startsAt asc")
    Page<ActivitySlotEntity> findBookableByPlaceIdAfter(@Param("placeId") UUID placeId, @Param("after") Instant after,
            Pageable pageable);
}
