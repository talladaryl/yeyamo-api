package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataStoryRepository extends JpaRepository<StoryEntity, UUID> {
    
    // Trouver les stories actives (non expirées, non supprimées) d'une liste d'auteurs
    @Query("""
        SELECT s FROM StoryEntity s 
        WHERE s.authorId IN :authorIds 
        AND s.expiresAt > :now 
        AND s.deletedAt IS NULL
        ORDER BY s.createdAt DESC
        """)
    List<StoryEntity> findActiveStoriesByAuthors(@Param("authorIds") List<String> authorIds, @Param("now") Instant now);
    
    // Trouver une story active (pour GET /stories/{id})
    @Query("""
        SELECT s FROM StoryEntity s 
        WHERE s.id = :id 
        AND s.expiresAt > :now 
        AND s.deletedAt IS NULL
        """)
    StoryEntity findActiveById(@Param("id") UUID id, @Param("now") Instant now);
    
    // Trouver les stories expirées (pour le job de nettoyage)
    @Query("""
        SELECT s FROM StoryEntity s 
        WHERE s.expiresAt <= :now 
        AND s.deletedAt IS NULL
        """)
    List<StoryEntity> findExpiredStories(@Param("now") Instant now);
    
    // Trouver les stories d'un auteur (pour profil)
    List<StoryEntity> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(String authorId);
}
