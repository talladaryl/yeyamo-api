package com.yeyamo_mobile.api.content_service.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.content_service.application.StoryService;

/**
 * Job scheduled pour marquer les stories expirées.
 * S'exécute toutes les heures pour nettoyer les stories dont expires_at est dépassé.
 */
@Component
@ConditionalOnProperty(name = "content.story.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class StoryExpirationScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(StoryExpirationScheduler.class);
    
    private final StoryService storyService;

    public StoryExpirationScheduler(StoryService storyService) {
        this.storyService = storyService;
    }

    /**
     * Marque les stories expirées (soft delete) toutes les heures.
     * Les stories sont marquées comme supprimées plutôt que supprimées immédiatement
     * pour garder l'historique des vues cohérent.
     */
    @Scheduled(fixedDelayString = "${content.story.expiration.delay-ms:3600000}") // 1 heure par défaut
    public void markExpiredStories() {
        try {
            int marked = storyService.markExpiredStories();
            if (marked > 0) {
                log.info("Marked {} expired stories as deleted", marked);
            }
        } catch (Exception e) {
            log.error("Failed to mark expired stories", e);
        }
    }
}
