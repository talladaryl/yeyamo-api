package com.yeyamo_mobile.api.booking_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceReadModelRepository extends JpaRepository<PlaceReadModelEntity, UUID> {
    Optional<PlaceReadModelEntity> findByPlaceIdAndActiveTrue(UUID placeId);
}
