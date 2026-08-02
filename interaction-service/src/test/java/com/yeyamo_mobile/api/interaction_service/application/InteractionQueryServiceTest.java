package com.yeyamo_mobile.api.interaction_service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.interaction_service.application.port.InteractionCachePort;
import com.yeyamo_mobile.api.interaction_service.domain.model.RelationType;
import com.yeyamo_mobile.api.interaction_service.domain.port.*;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository;

class InteractionQueryServiceTest {

    @Test
    void readsCountersFromDatabaseOnCacheMissAndPopulatesCache() {
        UUID postId = UUID.randomUUID();
        RelationRepository relations = mock(RelationRepository.class);
        CommentRepository comments = mock(CommentRepository.class);
        ShareRepository shares = mock(ShareRepository.class);
        CheckInRepository checkIns = mock(CheckInRepository.class);
        InteractionCachePort cache = mock(InteractionCachePort.class);
        when(cache.getCounts(postId)).thenReturn(Optional.empty());
        when(relations.count(postId, RelationType.LIKE)).thenReturn(12L);
        when(comments.countActiveByPost(postId)).thenReturn(3L);
        when(shares.countByPost(postId)).thenReturn(2L);
        when(relations.find(postId, "viewer", RelationType.LIKE)).thenReturn(Optional.empty());
        when(relations.find(postId, "viewer", RelationType.FAVORITE)).thenReturn(Optional.empty());

        InteractionSummary result = new InteractionQueryService(
                relations, comments, shares, checkIns, mock(SpringReviewRepository.class), cache)
                .summary(postId, "viewer");

        assertEquals(12, result.likes());
        assertEquals(3, result.comments());
        assertEquals(2, result.shares());
        verify(cache).putCounts(postId, new InteractionSummary.Counts(12, 3, 2));
    }

    @Test
    void cacheHitAvoidsDatabaseCounterQueriesButStillReadsViewerState() {
        UUID postId = UUID.randomUUID();
        RelationRepository relations = mock(RelationRepository.class);
        CommentRepository comments = mock(CommentRepository.class);
        ShareRepository shares = mock(ShareRepository.class);
        CheckInRepository checkIns = mock(CheckInRepository.class);
        InteractionCachePort cache = mock(InteractionCachePort.class);
        when(cache.getCounts(postId)).thenReturn(Optional.of(new InteractionSummary.Counts(5, 4, 3)));

        InteractionSummary result = new InteractionQueryService(
                relations, comments, shares, checkIns, mock(SpringReviewRepository.class), cache)
                .summary(postId, null);

        assertEquals(5, result.likes());
        verify(relations, never()).count(any(), any());
        verify(comments, never()).countActiveByPost(any());
        verify(shares, never()).countByPost(any());
    }
}
