package com.yeyamo_mobile.api.place_service.repository;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yeyamo_mobile.api.place_service.models.PartnerReadModel;
public interface PartnerReadModelRepository extends JpaRepository<PartnerReadModel, UUID> {
    Optional<PartnerReadModel> findByOwnerUserId(String ownerUserId);
}
