package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataStoryViewRepository extends JpaRepository<StoryViewEntity, StoryViewEntity.StoryViewId> {
    
    // Compter les vues d'une story
    long countByStoryId(UUID storyId);
    
    // Récupérer les viewers d'une story
    List<StoryViewEntity> findByStoryIdOrderByViewedAtDesc(UUID storyId);
    
    // Vérifier si un utilisateur a déjà vu une story
    boolean existsByStoryIdAndViewerId(UUID storyId, String viewerId);
    
    // Récupérer les IDs des stories vues par un utilisateur
    @Query("SELECT sv.storyId FROM StoryViewEntity sv WHERE sv.viewerId = :viewerId")
    List<UUID> findStoryIdsByViewer(String viewerId);
}
