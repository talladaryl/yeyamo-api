package com.yeyamo_mobile.api.content_service.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.content_service.infrastructure.client.UserServiceClient;
import com.yeyamo_mobile.api.content_service.infrastructure.outbox.ContentOutboxPort;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.*;

@Service
public class StoryService {
    
    private final SpringDataStoryRepository storyRepository;
    private final SpringDataStoryViewRepository viewRepository;
    private final UserServiceClient userServiceClient;
    private final ContentOutboxPort outbox;

    public StoryService(
            SpringDataStoryRepository storyRepository,
            SpringDataStoryViewRepository viewRepository,
            UserServiceClient userServiceClient,
            ContentOutboxPort outbox) {
        this.storyRepository = storyRepository;
        this.viewRepository = viewRepository;
        this.userServiceClient = userServiceClient;
        this.outbox = outbox;
    }

    // ─── CRÉATION ───────────────────────────────────────────────────────────────

    @Transactional
    public StoryEntity create(String authorId, UUID mediaId, String caption, int durationSeconds, String correlationId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS); // Stories expirent après 24h

        StoryEntity story = new StoryEntity();
        story.setId(UUID.randomUUID());
        story.setAuthorId(authorId);
        story.setMediaId(mediaId);
        story.setCaption(caption);
        story.setDurationSeconds(durationSeconds > 0 ? durationSeconds : 15);
        story.setCreatedAt(now);
        story.setExpiresAt(expiresAt);

        StoryEntity saved = storyRepository.save(story);

        // Événement Kafka
        outbox.append("content.story.created", saved.getId().toString(), authorId, correlationId,
                java.util.Map.of("storyId", saved.getId().toString(), "authorId", authorId, "mediaId", mediaId.toString()));

        return saved;
    }

    // ─── LECTURE ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StoryWithViews> getActiveStoriesForUser(String userId) {
        // 1. Récupérer la liste des comptes suivis depuis user-service
        List<String> followingIds = userServiceClient.getFollowingIds(userId);
        
        if (followingIds.isEmpty()) {
            return List.of(); // Pas de comptes suivis = pas de stories
        }

        // 2. Récupérer les stories actives de ces auteurs
        Instant now = Instant.now();
        List<StoryEntity> stories = storyRepository.findActiveStoriesByAuthors(followingIds, now);

        // 3. Pour chaque story, récupérer le nombre de vues
        return stories.stream()
                .map(story -> {
                    long viewCount = viewRepository.countByStoryId(story.getId());
                    boolean viewedByMe = viewRepository.existsByStoryIdAndViewerId(story.getId(), userId);
                    return new StoryWithViews(story, viewCount, viewedByMe);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoryEntity getActiveStory(UUID storyId) {
        Instant now = Instant.now();
        StoryEntity story = storyRepository.findActiveById(storyId, now);
        
        if (story == null) {
            throw new ContentException("STORY_NOT_FOUND", "Story introuvable ou expirée");
        }
        
        return story;
    }

    // ─── VUES ───────────────────────────────────────────────────────────────────

    @Transactional
    public void recordView(UUID storyId, String viewerId, String correlationId) {
        // Vérifier que la story existe et est active
        StoryEntity story = getActiveStory(storyId);

        // Idempotence : ne pas dupliquer si déjà vu
        StoryViewEntity.StoryViewId viewId = new StoryViewEntity.StoryViewId(storyId, viewerId);
        
        if (viewRepository.existsById(viewId)) {
            return; // Déjà vu, succès silencieux
        }

        // Enregistrer la vue
        StoryViewEntity view = new StoryViewEntity();
        view.setStoryId(storyId);
        view.setViewerId(viewerId);
        view.setViewedAt(Instant.now());
        viewRepository.save(view);

        // Événement Kafka
        outbox.append("content.story.viewed", storyId.toString(), viewerId, correlationId,
                java.util.Map.of("storyId", storyId.toString(), "viewerId", viewerId, "authorId", story.getAuthorId()));
    }

    @Transactional(readOnly = true)
    public long getViewCount(UUID storyId) {
        return viewRepository.countByStoryId(storyId);
    }

    // ─── SUPPRESSION ────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID storyId, String authorId, String correlationId) {
        StoryEntity story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ContentException("STORY_NOT_FOUND", "Story introuvable"));

        // Vérifier la propriété
        if (!story.getAuthorId().equals(authorId)) {
            throw new ContentException("STORY_FORBIDDEN", "Vous n'êtes pas l'auteur de cette story");
        }

        // Soft delete
        story.setDeletedAt(Instant.now());
        storyRepository.save(story);

        // Événement Kafka
        outbox.append("content.story.deleted", storyId.toString(), authorId, correlationId,
                java.util.Map.of("storyId", storyId.toString(), "authorId", authorId));
    }

    // ─── NETTOYAGE (appelé par le scheduler) ───────────────────────────────────

    @Transactional
    public int markExpiredStories() {
        Instant now = Instant.now();
        List<StoryEntity> expiredStories = storyRepository.findExpiredStories(now);
        
        for (StoryEntity story : expiredStories) {
            story.setDeletedAt(now);
            storyRepository.save(story);
        }
        
        return expiredStories.size();
    }

    // ─── NESTED CLASSES ─────────────────────────────────────────────────────────

    public record StoryWithViews(StoryEntity story, long viewCount, boolean viewedByMe) {}
}
