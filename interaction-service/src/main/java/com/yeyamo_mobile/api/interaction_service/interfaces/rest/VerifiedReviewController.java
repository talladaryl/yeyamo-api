package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.interaction_service.application.VerifiedReviewService;
import com.yeyamo_mobile.api.interaction_service.domain.model.ReviewTargetType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/reviews")
public class VerifiedReviewController {
    private final VerifiedReviewService reviews;
    public VerifiedReviewController(VerifiedReviewService reviews) { this.reviews = reviews; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody VerifiedReviewRequest request, Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return ReviewResponse.from(reviews.create(request.targetType(), request.targetId(), authentication.getName(),
                request.rating().shortValue(), request.comment(), key, correlation));
    }

    @PutMapping("/{id}")
    public ReviewResponse update(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request, Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return ReviewResponse.from(reviews.update(id, authentication.getName(), request.rating().shortValue(), request.comment(), key, correlation));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        reviews.delete(id, authentication.getName(), key, correlation);
    }

    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable UUID id, Authentication authentication,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        reviews.report(id, authentication.getName(), correlation);
    }

    @GetMapping("/{targetType}/{targetId}")
    public Page<ReviewResponse> list(@PathVariable ReviewTargetType targetType, @PathVariable UUID targetId,
            org.springframework.data.domain.Pageable pageable) {
        return reviews.publicReviews(targetType, targetId, pageable).map(ReviewResponse::from);
    }

    @GetMapping("/{targetType}/{targetId}/aggregate")
    public ReviewAggregateResponse aggregate(@PathVariable ReviewTargetType targetType, @PathVariable UUID targetId) {
        return reviews.aggregate(targetType, targetId);
    }
}
