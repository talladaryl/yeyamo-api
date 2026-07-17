package com.yeyamo_mobile.api.interaction_service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.yeyamo_mobile.api.interaction_service.application.port.CommandReceiptPort;
import com.yeyamo_mobile.api.interaction_service.application.port.InteractionCachePort;
import com.yeyamo_mobile.api.interaction_service.application.port.InteractionOutboxPort;
import com.yeyamo_mobile.api.interaction_service.domain.model.CommandReceipt;
import com.yeyamo_mobile.api.interaction_service.domain.port.*;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private RelationRepository relations;
    @Mock private CommentRepository comments;
    @Mock private ShareRepository shares;
    @Mock private CheckInRepository checkIns;
    @Mock private SpringReviewRepository reviews;
    @Mock private CommandReceiptPort receipts;
    @Mock private InteractionOutboxPort outbox;
    @Mock private InteractionCachePort cache;

    private InteractionCommandService commandService;
    private InteractionQueryService queryService;

    private UUID placeId;
    private UUID reviewId;
    private String userId;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        commandService = new InteractionCommandService(
            relations, comments, shares, checkIns, reviews, receipts, outbox, cache
        );
        queryService = new InteractionQueryService(
            relations, comments, shares, checkIns, reviews, cache
        );

        placeId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        userId = "user-123";
        otherUserId = "user-456";
    }

    // ─── CRÉATION ────────────────────────────────────────────────────────────────

    @Test
    void shouldCreateReview() {
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.existsByUserIdAndPlaceId(userId, placeId)).thenReturn(false);
        
        ReviewEntity savedReview = createReviewEntity(reviewId, userId, placeId, (short) 5, "Great place!");
        when(reviews.save(any(ReviewEntity.class))).thenReturn(savedReview);
        when(receipts.save(any(CommandReceipt.class))).thenReturn(null);

        ReviewEntity result = commandService.createReview(placeId, userId, (short) 5, "Great place!", "key-1", "corr-1");

        assertNotNull(result);
        assertEquals(placeId, result.getPlaceId());
        assertEquals((short) 5, result.getRating());
        verify(reviews).save(any(ReviewEntity.class));
        verify(outbox).append(eq("interaction.review.created"), anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailCreateReviewWhenDuplicate() {
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.existsByUserIdAndPlaceId(userId, placeId)).thenReturn(true);

        InteractionException ex = assertThrows(InteractionException.class,
            () -> commandService.createReview(placeId, userId, (short) 4, "Comment", "key-1", "corr-1"));

        assertEquals("REVIEW_ALREADY_EXISTS", ex.getCode());
        assertTrue(ex.getMessage().contains("PUT"));
        verify(reviews, never()).save(any());
    }

    @Test
    void shouldBeIdempotentOnCreate() {
        ReviewEntity savedReview = createReviewEntity(reviewId, userId, placeId, (short) 5, "Great!");
        CommandReceipt receipt = mock(CommandReceipt.class);
        when(receipt.resultId()).thenReturn(reviewId);
        
        when(receipts.find("key-1", userId, "REVIEW_CREATE:" + placeId)).thenReturn(Optional.of(receipt));
        when(reviews.findById(reviewId)).thenReturn(Optional.of(savedReview));

        ReviewEntity result = commandService.createReview(placeId, userId, (short) 5, "Great!", "key-1", "corr-1");

        assertNotNull(result);
        verify(reviews, never()).save(any()); // Should not create again
        verify(outbox, never()).append(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    // ─── MISE À JOUR ─────────────────────────────────────────────────────────────

    @Test
    void shouldUpdateOwnReview() {
        ReviewEntity existingReview = createReviewEntity(reviewId, userId, placeId, (short) 4, "Good");
        
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviews.save(any(ReviewEntity.class))).thenReturn(existingReview);
        when(receipts.save(any(CommandReceipt.class))).thenReturn(null);

        ReviewEntity result = commandService.updateReview(reviewId, userId, (short) 5, "Excellent!", "key-2", "corr-2");

        assertEquals((short) 5, result.getRating());
        assertEquals("Excellent!", result.getComment());
        verify(reviews).save(existingReview);
        verify(outbox).append(eq("interaction.review.updated"), anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailUpdateReviewAsNonAuthor() {
        ReviewEntity existingReview = createReviewEntity(reviewId, userId, placeId, (short) 4, "Good");
        
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.findById(reviewId)).thenReturn(Optional.of(existingReview));

        InteractionException ex = assertThrows(InteractionException.class,
            () -> commandService.updateReview(reviewId, otherUserId, (short) 5, "Hacked", "key-2", "corr-2"));

        assertEquals("INTERACTION_FORBIDDEN", ex.getCode());
        verify(reviews, never()).save(any());
    }

    @Test
    void shouldFailUpdateNonExistentReview() {
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.findById(reviewId)).thenReturn(Optional.empty());

        InteractionException ex = assertThrows(InteractionException.class,
            () -> commandService.updateReview(reviewId, userId, (short) 5, "Update", "key-2", "corr-2"));

        assertEquals("REVIEW_NOT_FOUND", ex.getCode());
    }

    // ─── SUPPRESSION ─────────────────────────────────────────────────────────────

    @Test
    void shouldDeleteOwnReview() {
        ReviewEntity existingReview = createReviewEntity(reviewId, userId, placeId, (short) 4, "Good");
        
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(receipts.save(any(CommandReceipt.class))).thenReturn(null);

        assertDoesNotThrow(() -> commandService.deleteReview(reviewId, userId, false, "key-3", "corr-3"));

        verify(reviews).deleteById(reviewId);
        verify(outbox).append(eq("interaction.review.deleted"), anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldDeleteReviewAsModerator() {
        ReviewEntity existingReview = createReviewEntity(reviewId, otherUserId, placeId, (short) 1, "Spam");
        
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(receipts.save(any(CommandReceipt.class))).thenReturn(null);

        // Moderator (userId) deletes someone else's review (otherUserId)
        assertDoesNotThrow(() -> commandService.deleteReview(reviewId, userId, true, "key-3", "corr-3"));

        verify(reviews).deleteById(reviewId);
        verify(outbox).append(eq("interaction.review.deleted"), anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailDeleteReviewAsNonAuthorNonModerator() {
        ReviewEntity existingReview = createReviewEntity(reviewId, userId, placeId, (short) 4, "Good");
        
        when(receipts.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(reviews.findById(reviewId)).thenReturn(Optional.of(existingReview));

        InteractionException ex = assertThrows(InteractionException.class,
            () -> commandService.deleteReview(reviewId, otherUserId, false, "key-3", "corr-3"));

        assertEquals("INTERACTION_FORBIDDEN", ex.getCode());
        verify(reviews, never()).deleteById(any());
    }

    // ─── REQUÊTES ────────────────────────────────────────────────────────────────

    @Test
    void shouldGetReviewsByPlace() {
        ReviewEntity review1 = createReviewEntity(UUID.randomUUID(), userId, placeId, (short) 5, "Great!");
        ReviewEntity review2 = createReviewEntity(UUID.randomUUID(), otherUserId, placeId, (short) 4, "Good");
        
        when(reviews.findByPlaceIdOrderByCreatedAtDesc(eq(placeId), any(PageRequest.class)))
            .thenReturn(List.of(review1, review2));

        List<ReviewEntity> result = queryService.reviewsByPlace(placeId, 50);

        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals((short) 5, result.get(0).getRating());
    }

    @Test
    void shouldGetReviewsByUser() {
        ReviewEntity review1 = createReviewEntity(UUID.randomUUID(), userId, placeId, (short) 5, "Great!");
        ReviewEntity review2 = createReviewEntity(UUID.randomUUID(), userId, UUID.randomUUID(), (short) 4, "Good");
        
        when(reviews.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
            .thenReturn(List.of(review1, review2));

        List<ReviewEntity> result = queryService.reviewsByUser(userId, 50);

        assertEquals(2, result.size());
        assertEquals(placeId, result.get(0).getPlaceId());
    }

    @Test
    void shouldRespectPaginationLimit() {
        when(reviews.findByPlaceIdOrderByCreatedAtDesc(eq(placeId), any(PageRequest.class)))
            .thenReturn(List.of());

        queryService.reviewsByPlace(placeId, 200); // Request 200, should cap at 100

        verify(reviews).findByPlaceIdOrderByCreatedAtDesc(eq(placeId), eq(PageRequest.of(0, 100)));
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private ReviewEntity createReviewEntity(UUID id, String userId, UUID placeId, short rating, String comment) {
        ReviewEntity entity = new ReviewEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setPlaceId(placeId);
        entity.setRating(rating);
        entity.setComment(comment);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
