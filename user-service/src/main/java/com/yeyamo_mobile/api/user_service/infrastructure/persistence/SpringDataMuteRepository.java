package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataMuteRepository extends JpaRepository<MuteEntity, MuteEntity.MuteId> {
    boolean existsByIdMuterIdAndIdMutedId(UUID muterId, UUID mutedId);
    @Query("select m.id.mutedId from MuteEntity m where m.id.muterId = :muterId")
    List<UUID> findMutedIds(UUID muterId);
}
