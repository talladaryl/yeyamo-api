package com.yeyamo_mobile.api.interaction_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.interaction_service.application.port.CommandReceiptPort;
import com.yeyamo_mobile.api.interaction_service.application.port.InteractionOutboxPort;
import com.yeyamo_mobile.api.interaction_service.domain.model.ReviewTargetType;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringCheckInRepository;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VerifiedReviewEligibilityTest {
    private final SpringReviewRepository reviews = mock(SpringReviewRepository.class);
    private final SpringCheckInRepository checkIns = mock(SpringCheckInRepository.class);
    private final CommandReceiptPort receipts = mock(CommandReceiptPort.class);
    private final InteractionOutboxPort outbox = mock(InteractionOutboxPort.class);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final VerifiedReviewService service = new VerifiedReviewService(
            reviews, checkIns, receipts, outbox, builder,
            "http://places", "http://bookings", "http://events", "internal-token");

    @Test
    void futureEventIsNotEligible() {
        UUID targetId = UUID.randomUUID();
        respond(targetId, false, null, "EVENT_NOT_COMPLETED");

        var result = service.eligibility(ReviewTargetType.EVENT, targetId, "auth-user");

        assertFalse(result.eligible());
        assertEquals("EVENT_NOT_COMPLETED", result.reasonCode());
        server.verify();
    }

    @Test
    void completedEventWithoutConfirmedRegistrationIsNotEligible() {
        UUID targetId = UUID.randomUUID();
        respond(targetId, false, null, "REGISTRATION_NOT_CONFIRMED");

        var result = service.eligibility(ReviewTargetType.EVENT, targetId, "auth-user");

        assertFalse(result.eligible());
        assertEquals("REGISTRATION_NOT_CONFIRMED", result.reasonCode());
        server.verify();
    }

    @Test
    void completedEventWithConfirmedRegistrationCreatesReview() {
        UUID targetId = UUID.randomUUID();
        respond(targetId, true, targetId.toString(), "ELIGIBLE");
        when(receipts.find(any(), any(), any())).thenReturn(Optional.empty());
        when(reviews.existsByUserIdAndTargetTypeAndEvidenceReference(any(), any(), any())).thenReturn(false);
        when(reviews.save(any(ReviewEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewEntity review = service.create(ReviewTargetType.EVENT, targetId, "auth-user", (short) 5,
                "Excellent", "review-key", "correlation");

        assertTrue(review.getId() != null);
        assertEquals(targetId, review.getPlaceId());
        server.verify();
    }

    private void respond(UUID targetId, boolean eligible, String transactionId, String reasonCode) {
        String transactionJson = transactionId == null ? "null" : "\"" + transactionId + "\"";
        server.expect(requestTo("http://events/internal/review-eligibility/events/" + targetId + "/users/auth-user"))
                .andRespond(withSuccess("{\"eligible\":" + eligible + ",\"transactionId\":" + transactionJson
                        + ",\"reasonCode\":\"" + reasonCode + "\"}", MediaType.APPLICATION_JSON));
    }
}
