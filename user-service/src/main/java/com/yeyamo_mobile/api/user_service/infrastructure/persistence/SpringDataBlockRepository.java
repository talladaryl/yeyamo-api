package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataBlockRepository extends JpaRepository<BlockEntity, BlockEntity.BlockId> {
    
    boolean existsByIdBlockerIdAndIdBlockedId(UUID blockerId, UUID blockedId);

    @Query("SELECT b.id.blockedId FROM BlockEntity b WHERE b.id.blockerId = :userId")
    List<UUID> findBlockedIds(UUID userId);

    @Query("SELECT b.id.blockerId FROM BlockEntity b WHERE b.id.blockedId = :userId")
    List<UUID> findBlockerIds(UUID userId);

    // Vérifier si l'un des deux a bloqué l'autre (dans les deux sens)
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END 
        FROM BlockEntity b 
        WHERE (b.id.blockerId = :userId1 AND b.id.blockedId = :userId2)
        OR (b.id.blockerId = :userId2 AND b.id.blockedId = :userId1)
        """)
    boolean existsBlockInEitherDirection(UUID userId1, UUID userId2);
}
