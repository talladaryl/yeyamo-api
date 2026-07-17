package com.yeyamo_mobile.api.content_service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeyamo_mobile.api.content_service.infrastructure.client.UserServiceClient;
import com.yeyamo_mobile.api.content_service.infrastructure.outbox.ContentOutboxPort;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock private SpringDataStoryRepository storyRepository;
    @Mock private SpringDataStoryViewRepository viewRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private ContentOutboxPort outbox;

    @InjectMocks private StoryService service;

    private UUID storyId;
    private UUID mediaId;
    private String authorId;
    private String viewerId;
    private StoryEntity story;

    @BeforeEach
    void setUp() {
        storyId = UUID.randomUUID();
        mediaId = UUID.randomUUID();
        authorId = "author-123";
        viewerId = "viewer-456";
        
        story = new StoryEntity();
        story.setId(storyId);
        story.setAuthorId(authorId);
        story.setMediaId(mediaId);
        story.setCaption("Test story");
        story.setDurationSeconds(15);
        story.setCreatedAt(Instant.now());
        story.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
    }

    // ─── TESTS CRÉATION ─────────────────────────────────────────────────────────

    @Test
    void shouldCreateStory() {
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(story);

        StoryEntity result = service.create(authorId, mediaId, "Test", 15, "corr-1");

        assertNotNull(result);
        verify(storyRepository).save(any(StoryEntity.class));
        verify(outbox).append(eq("content.story.created"), anyString(), anyString(), anyString(), anyMap());
    }

    // ─── TESTS LECTURE ──────────────────────────────────────────────────────────

    @Test
    void shouldGetActiveStoriesForUser() {
        List<String> followingIds = List.of("user1", "user2");
        when(userServiceClient.getFollowingIds(viewerId)).thenReturn(followingIds);
        when(storyRepository.findActiveStoriesByAuthors(eq(followingIds), any(Instant.class)))
                .thenReturn(List.of(story));
        when(viewRepository.countByStoryId(storyId)).thenReturn(5L);
        when(viewRepository.existsByStoryIdAndViewerId(storyId, viewerId)).thenReturn(false);

        List<StoryService.StoryWithViews> result = service.getActiveStoriesForUser(viewerId);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).viewCount());
        assertFalse(result.get(0).viewedByMe());
    }

    @Test
    void shouldReturnEmptyWhenNoFollowing() {
        when(userServiceClient.getFollowingIds(viewerId)).thenReturn(List.of());

        List<StoryService.StoryWithViews> result = service.getActiveStoriesForUser(viewerId);

        assertTrue(result.isEmpty());
        verify(storyRepository, never()).findActiveStoriesByAuthors(anyList(), any());
    }

    @Test
    void shouldGetActiveStory() {
        when(storyRepository.findActiveById(eq(storyId), any(Instant.class))).thenReturn(story);

        StoryEntity result = service.getActiveStory(storyId);

        assertNotNull(result);
        assertEquals(storyId, result.getId());
    }

    @Test
    void shouldThrow404WhenStoryExpired() {
        when(storyRepository.findActiveById(eq(storyId), any(Instant.class))).thenReturn(null);

        ContentException ex = assertThrows(ContentException.class,
                () -> service.getActiveStory(storyId));

        assertEquals("STORY_NOT_FOUND", ex.getCode());
    }

    // ─── TESTS VUES ─────────────────────────────────────────────────────────────

    @Test
    void shouldRecordView() {
        when(storyRepository.findActiveById(eq(storyId), any(Instant.class))).thenReturn(story);
        when(viewRepository.existsById(any(StoryViewEntity.StoryViewId.class))).thenReturn(false);

        assertDoesNotThrow(() -> service.recordView(storyId, viewerId, "corr-1"));

        verify(viewRepository).save(any(StoryViewEntity.class));
        verify(outbox).append(eq("content.story.viewed"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldBeIdempotentWhenViewExists() {
        when(storyRepository.findActiveById(eq(storyId), any(Instant.class))).thenReturn(story);
        when(viewRepository.existsById(any(StoryViewEntity.StoryViewId.class))).thenReturn(true);

        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> service.recordView(storyId, viewerId, "corr-1"));

        // Ne doit pas sauvegarder ni publier d'événement
        verify(viewRepository, never()).save(any());
        verify(outbox, never()).append(anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailRecordViewForExpiredStory() {
        when(storyRepository.findActiveById(eq(storyId), any(Instant.class))).thenReturn(null);

        ContentException ex = assertThrows(ContentException.class,
                () -> service.recordView(storyId, viewerId, "corr-1"));

        assertEquals("STORY_NOT_FOUND", ex.getCode());
        verify(viewRepository, never()).save(any());
    }

    // ─── TESTS SUPPRESSION ──────────────────────────────────────────────────────

    @Test
    void shouldDeleteStoryAsAuthor() {
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(story);

        assertDoesNotThrow(() -> service.delete(storyId, authorId, "corr-1"));

        verify(storyRepository).save(any(StoryEntity.class));
        verify(outbox).append(eq("content.story.deleted"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailDeleteAsNonAuthor() {
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));

        ContentException ex = assertThrows(ContentException.class,
                () -> service.delete(storyId, "other-user", "corr-1"));

        assertEquals("STORY_FORBIDDEN", ex.getCode());
        verify(storyRepository, never()).save(any());
    }

    // ─── TESTS EXPIRATION ───────────────────────────────────────────────────────

    @Test
    void shouldMarkExpiredStories() {
        StoryEntity expiredStory1 = new StoryEntity();
        expiredStory1.setId(UUID.randomUUID());
        expiredStory1.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        StoryEntity expiredStory2 = new StoryEntity();
        expiredStory2.setId(UUID.randomUUID());
        expiredStory2.setExpiresAt(Instant.now().minus(2, ChronoUnit.HOURS));

        when(storyRepository.findExpiredStories(any(Instant.class)))
                .thenReturn(List.of(expiredStory1, expiredStory2));
        when(storyRepository.save(any(StoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int marked = service.markExpiredStories();

        assertEquals(2, marked);
        verify(storyRepository, times(2)).save(any(StoryEntity.class));
    }

    @Test
    void shouldReturnZeroWhenNoExpiredStories() {
        when(storyRepository.findExpiredStories(any(Instant.class))).thenReturn(List.of());

        int marked = service.markExpiredStories();

        assertEquals(0, marked);
        verify(storyRepository, never()).save(any());
    }
}
