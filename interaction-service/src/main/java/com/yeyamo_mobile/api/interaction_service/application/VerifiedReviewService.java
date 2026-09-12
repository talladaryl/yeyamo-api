package com.yeyamo_mobile.api.interaction_service.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.interaction_service.application.port.CommandReceiptPort;
import com.yeyamo_mobile.api.interaction_service.application.port.InteractionOutboxPort;
import com.yeyamo_mobile.api.interaction_service.domain.model.CommandReceipt;
import com.yeyamo_mobile.api.interaction_service.domain.model.ReviewTargetType;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.CheckInEntity;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringCheckInRepository;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository;
import com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewAggregateResponse;

@Service
public class VerifiedReviewService {
    private final SpringReviewRepository reviews;
    private final SpringCheckInRepository checkIns;
    private final CommandReceiptPort receipts;
    private final InteractionOutboxPort outbox;
    private final RestClient places;
    private final RestClient bookings;
    private final RestClient events;
    private final String internalToken;

    public VerifiedReviewService(SpringReviewRepository reviews, SpringCheckInRepository checkIns,
            CommandReceiptPort receipts, InteractionOutboxPort outbox, RestClient.Builder builder,
            @Value("${yeyamo.services.place.url:http://place-service:8093}") String placeUrl,
            @Value("${yeyamo.services.booking.url:http://booking-service:8086}") String bookingUrl,
            @Value("${yeyamo.services.event.url:http://event-service:8090}") String eventUrl,
            @Value("${yeyamo.security.internal-token:${INTERNAL_SERVICE_TOKEN:}}") String internalToken) {
        this.reviews = reviews;
        this.checkIns = checkIns;
        this.receipts = receipts;
        this.outbox = outbox;
        this.places = builder.baseUrl(placeUrl).build();
        this.bookings = builder.baseUrl(bookingUrl).build();
        this.events = builder.baseUrl(eventUrl).build();
        this.internalToken = internalToken;
    }

    @Transactional
    public ReviewEntity create(ReviewTargetType targetType, UUID targetId, String actor, short rating, String comment,
            String idempotencyKey, String correlationId) {
        String operation = "VERIFIED_REVIEW_CREATE:" + targetType.name() + ":" + targetId;
        var replay = receipts.find(idempotencyKey, actor, operation);
        if (replay.isPresent()) return required(replay.get().resultId());

        Eligibility eligibility = verify(targetType, targetId, actor);
        if (reviews.existsByUserIdAndPlaceId(actor, targetId)) {
            throw new InteractionException("REVIEW_ALREADY_EXISTS", "Vous avez deja publie un avis pour cette cible");
        }
        ReviewEntity review = new ReviewEntity();
        review.setId(UUID.randomUUID());
        review.setUserId(actor);
        // place_id is the historical physical target column; it is retained for backward compatibility.
        review.setPlaceId(targetId);
        review.setTargetType(targetType.name());
        review.setEvidenceReference(eligibility.reference());
        review.setRating(rating);
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        review.setStatus("ACTIVE");
        review.setCreatedAt(Instant.now());
        review.setUpdatedAt(review.getCreatedAt());
        review = reviews.save(review);
        receipts.save(CommandReceipt.create(idempotencyKey, actor, operation, review.getId(), true));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", review.getId());
        payload.put("targetType", targetType.name());
        payload.put("targetId", targetId);
        payload.put("userId", actor);
        payload.put("rating", rating);
        payload.put("evidenceReference", eligibility.reference());
        outbox.append("interaction.review.created", "review", review.getId().toString(), actor, correlationId, payload);
        return review;
    }

    @Transactional
    public ReviewEntity update(UUID reviewId, String actor, short rating, String comment, String idempotencyKey, String correlationId) {
        String operation = "VERIFIED_REVIEW_UPDATE:" + reviewId;
        var replay = receipts.find(idempotencyKey, actor, operation);
        if (replay.isPresent()) return required(reviewId);
        ReviewEntity review = required(reviewId);
        if (!actor.equals(review.getUserId())) throw new InteractionException("INTERACTION_FORBIDDEN", "Seul l'auteur peut modifier cet avis");
        if (!"ACTIVE".equals(review.getStatus())) throw new InteractionException("REVIEW_NOT_EDITABLE", "Cet avis n'est plus modifiable");
        review.setRating(rating);
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        review.setUpdatedAt(Instant.now());
        review = reviews.save(review);
        receipts.save(CommandReceipt.create(idempotencyKey, actor, operation, reviewId, true));
        outbox.append("interaction.review.updated", "review", reviewId.toString(), actor, correlationId,
                Map.of("reviewId", reviewId, "targetType", review.getTargetType(), "targetId", review.getPlaceId(), "rating", rating));
        return review;
    }

    @Transactional
    public void report(UUID reviewId, String actor, String correlationId) {
        ReviewEntity review = required(reviewId);
        if (actor.equals(review.getUserId())) throw new InteractionException("REVIEW_REPORT_INVALID", "Vous ne pouvez pas signaler votre propre avis");
        review.setStatus("REPORTED");
        review.setReportedBy(actor);
        review.setReportedAt(Instant.now());
        reviews.save(review);
        outbox.append("interaction.review.reported", "review", reviewId.toString(), actor, correlationId,
                Map.of("reviewId", reviewId, "targetType", review.getTargetType(), "targetId", review.getPlaceId()));
    }

    @Transactional
    public void delete(UUID reviewId, String actor, String idempotencyKey, String correlationId) {
        String operation = "VERIFIED_REVIEW_DELETE:" + reviewId;
        if (receipts.find(idempotencyKey, actor, operation).isPresent()) return;
        ReviewEntity review = required(reviewId);
        if (!actor.equals(review.getUserId())) throw new InteractionException("INTERACTION_FORBIDDEN", "Seul l'auteur peut supprimer cet avis");
        if ("DELETED".equals(review.getStatus())) return;
        review.setStatus("DELETED");
        review.setDeletedAt(Instant.now());
        review.setDeletedBy(actor);
        review.setUpdatedAt(review.getDeletedAt());
        reviews.save(review);
        receipts.save(CommandReceipt.create(idempotencyKey, actor, operation, reviewId, true));
        outbox.append("interaction.review.deleted", "review", reviewId.toString(), actor, correlationId,
                Map.of("reviewId", reviewId, "targetType", review.getTargetType(), "targetId", review.getPlaceId()));
    }

    @Transactional(readOnly = true)
    public Page<ReviewEntity> publicReviews(ReviewTargetType type, UUID targetId, Pageable pageable) {
        Pageable bounded = PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.min(100, Math.max(1, pageable.getPageSize())), pageable.getSort().isSorted() ? pageable.getSort() : org.springframework.data.domain.Sort.by("createdAt").descending());
        return reviews.findByTargetTypeAndPlaceIdAndStatusOrderByCreatedAtDesc(type.name(), targetId, "ACTIVE", bounded);
    }

    @Transactional(readOnly = true)
    public ReviewAggregateResponse aggregate(ReviewTargetType type, UUID targetId) {
        Object[] values = reviews.aggregateActive(type.name(), targetId);
        long count = values == null || values.length == 0 || values[0] == null ? 0L : ((Number) values[0]).longValue();
        Double average = values == null || values.length < 2 || values[1] == null ? null : ((Number) values[1]).doubleValue();
        return new ReviewAggregateResponse(count, average);
    }

    private Eligibility verify(ReviewTargetType type, UUID targetId, String actor) {
        return switch (type) {
            case PLACE -> placeEligibility(targetId, actor);
            case EXPERIENCE -> remoteEligibility(bookings, "/internal/review-eligibility/experiences/" + targetId + "/users/" + actor);
            case EVENT -> remoteEligibility(events, "/internal/review-eligibility/events/" + targetId + "/users/" + actor);
            case ARTISAN -> throw new InteractionException("REVIEW_ELIGIBILITY_UNAVAILABLE", "La preuve canonique de transaction artisan n'est pas encore exposee");
        };
    }

    private Eligibility placeEligibility(UUID placeId, String actor) {
        CheckInEntity checkIn = checkIns.findFirstByUserIdAndCatalogAssetIdOrderByOccurredAtDesc(actor, placeId)
                .orElseThrow(() -> new InteractionException("REVIEW_ELIGIBILITY_REQUIRED", "Un check-in sur ce lieu est requis avant de publier un avis"));
        try {
            places.get().uri("/api/v1/places/{id}", placeId).retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new InteractionException("REVIEW_TARGET_NOT_FOUND", "Le lieu canonique est introuvable");
                    }).toBodilessEntity();
        } catch (InteractionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InteractionException("REVIEW_ELIGIBILITY_UNAVAILABLE", "La validation du lieu est indisponible");
        }
        return new Eligibility("checkin:" + checkIn.getId());
    }

    private Eligibility remoteEligibility(RestClient client, String uri) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new InteractionException("REVIEW_ELIGIBILITY_UNAVAILABLE", "Le jeton de service interne est indisponible");
        }
        try {
            RemoteEligibility result = client.get().uri(uri).header("X-Internal-Token", internalToken).retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new InteractionException("REVIEW_ELIGIBILITY_UNAVAILABLE", "La preuve de transaction est indisponible");
                    }).body(RemoteEligibility.class);
            if (result == null || !result.eligible() || result.transactionId() == null) {
                throw new InteractionException("REVIEW_ELIGIBILITY_REQUIRED", "Une interaction terminee et verifiee est requise avant de publier un avis");
            }
            return new Eligibility(result.transactionId());
        } catch (InteractionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InteractionException("REVIEW_ELIGIBILITY_UNAVAILABLE", "La preuve de transaction est indisponible");
        }
    }

    private ReviewEntity required(UUID id) { return reviews.findById(id).orElseThrow(() -> new InteractionException("REVIEW_NOT_FOUND", "Avis introuvable")); }
    private record Eligibility(String reference) { }
    private record RemoteEligibility(boolean eligible, String transactionId) { }
}
