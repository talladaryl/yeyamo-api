package com.yeyamo_mobile.api.campaign_service.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CampaignTest {

    @Test
    void create_shouldCreateCampaignInDraftStatus() {
        // Given
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(7, ChronoUnit.DAYS);
        TargetConfiguration targeting = createValidTargeting();
        CreativeConfiguration creative = createValidCreative();

        // When
        Campaign campaign = Campaign.create(
                "partner-123",
                "Summer Campaign",
                CampaignObjective.AWARENESS,
                PromotedEntityType.EVENT,
                "event-456",
                BillingModel.CPM,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "XAF",
                startAt,
                endAt,
                targeting,
                creative,
                "user-789"
        );

        // Then
        assertNotNull(campaign.getId());
        assertEquals("partner-123", campaign.getPartnerId());
        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        assertEquals(BigDecimal.ZERO, campaign.getSpentAmount());
        assertNotNull(campaign.getCreatedAt());
        assertNotNull(campaign.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenTotalBudgetIsZero() {
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(7, ChronoUnit.DAYS);

        assertThrows(IllegalArgumentException.class, () ->
                Campaign.create(
                        "partner-123",
                        "Campaign",
                        CampaignObjective.AWARENESS,
                        PromotedEntityType.EVENT,
                        "event-456",
                        BillingModel.CPM,
                        BigDecimal.ZERO,
                        new BigDecimal("100.00"),
                        "XAF",
                        startAt,
                        endAt,
                        createValidTargeting(),
                        createValidCreative(),
                        "user-789"
                )
        );
    }

    @Test
    void create_shouldThrowException_whenDailyBudgetExceedsTotalBudget() {
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(7, ChronoUnit.DAYS);

        assertThrows(IllegalArgumentException.class, () ->
                Campaign.create(
                        "partner-123",
                        "Campaign",
                        CampaignObjective.AWARENESS,
                        PromotedEntityType.EVENT,
                        "event-456",
                        BillingModel.CPM,
                        new BigDecimal("100.00"),
                        new BigDecimal("200.00"),
                        "XAF",
                        startAt,
                        endAt,
                        createValidTargeting(),
                        createValidCreative(),
                        "user-789"
                )
        );
    }

    @Test
    void create_shouldThrowException_whenEndAtIsBeforeStartAt() {
        Instant startAt = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant endAt = startAt.minus(1, ChronoUnit.DAYS);

        assertThrows(IllegalArgumentException.class, () ->
                Campaign.create(
                        "partner-123",
                        "Campaign",
                        CampaignObjective.AWARENESS,
                        PromotedEntityType.EVENT,
                        "event-456",
                        BillingModel.CPM,
                        new BigDecimal("1000.00"),
                        new BigDecimal("100.00"),
                        "XAF",
                        startAt,
                        endAt,
                        createValidTargeting(),
                        createValidCreative(),
                        "user-789"
                )
        );
    }

    @Test
    void submit_shouldTransitionFromDraftToPendingReview() {
        Campaign campaign = createCampaign();
        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());

        campaign.submit();

        assertEquals(CampaignStatus.PENDING_REVIEW, campaign.getStatus());
        assertNull(campaign.getRejectionReason());
    }

    @Test
    void submit_shouldThrowException_whenNotInDraftOrRejected() {
        Campaign campaign = createCampaign();
        campaign.submit();
        assertEquals(CampaignStatus.PENDING_REVIEW, campaign.getStatus());

        assertThrows(IllegalStateException.class, campaign::submit);
    }

    @Test
    void approve_shouldTransitionFromPendingReviewToApproved() {
        Campaign campaign = createCampaign();
        campaign.submit();

        campaign.approve("admin-123");

        assertEquals(CampaignStatus.APPROVED, campaign.getStatus());
        assertEquals("admin-123", campaign.getApprovedBy());
        assertNull(campaign.getRejectionReason());
    }

    @Test
    void reject_shouldTransitionFromPendingReviewToRejected() {
        Campaign campaign = createCampaign();
        campaign.submit();

        campaign.reject("Budget is too high");

        assertEquals(CampaignStatus.REJECTED, campaign.getStatus());
        assertEquals("Budget is too high", campaign.getRejectionReason());
    }

    @Test
    void activate_shouldTransitionFromApprovedToActive() {
        Campaign campaign = createCampaign();
        campaign.submit();
        campaign.approve("admin-123");

        campaign.activate();

        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
    }

    @Test
    void activate_shouldThrowException_whenCampaignIsExpired() {
        Instant startAt = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant endAt = Instant.now().minus(1, ChronoUnit.DAYS);
        
        Campaign campaign = Campaign.create(
                "partner-123",
                "Expired Campaign",
                CampaignObjective.AWARENESS,
                PromotedEntityType.EVENT,
                "event-456",
                BillingModel.CPM,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "XAF",
                startAt,
                endAt,
                createValidTargeting(),
                createValidCreative(),
                "user-789"
        );
        
        campaign.submit();
        campaign.approve("admin-123");

        assertThrows(IllegalStateException.class, campaign::activate);
    }

    @Test
    void pause_shouldTransitionFromActiveToPaused() {
        Campaign campaign = createActiveCampaign();

        campaign.pause();

        assertEquals(CampaignStatus.PAUSED, campaign.getStatus());
    }

    @Test
    void resume_shouldTransitionFromPausedToActive() {
        Campaign campaign = createActiveCampaign();
        campaign.pause();

        campaign.resume();

        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
    }

    @Test
    void cancel_shouldTransitionToCancelled() {
        Campaign campaign = createCampaign();

        campaign.cancel();

        assertEquals(CampaignStatus.CANCELLED, campaign.getStatus());
    }

    @Test
    void complete_shouldTransitionToCompleted() {
        Campaign campaign = createActiveCampaign();

        campaign.complete();

        assertEquals(CampaignStatus.COMPLETED, campaign.getStatus());
    }

    @Test
    void deductBudget_shouldUpdateSpentAmount() {
        Campaign campaign = createActiveCampaign();
        BigDecimal initialSpent = campaign.getSpentAmount();

        campaign.deductBudget(new BigDecimal("50.00"));

        assertEquals(initialSpent.add(new BigDecimal("50.00")), campaign.getSpentAmount());
    }

    @Test
    void deductBudget_shouldThrowException_whenExceedingTotalBudget() {
        Campaign campaign = createActiveCampaign();

        assertThrows(IllegalStateException.class, () ->
                campaign.deductBudget(new BigDecimal("2000.00"))
        );
    }

    @Test
    void deductBudget_shouldTransitionToBudgetExhausted_whenBudgetFull() {
        Campaign campaign = createActiveCampaign();

        campaign.deductBudget(new BigDecimal("1000.00"));

        assertEquals(CampaignStatus.BUDGET_EXHAUSTED, campaign.getStatus());
        assertTrue(campaign.isBudgetExhausted());
    }

    @Test
    void update_shouldUpdateCampaignFields_whenInDraft() {
        Campaign campaign = createCampaign();
        Instant newStartAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant newEndAt = newStartAt.plus(10, ChronoUnit.DAYS);

        campaign.update(
                "Updated Campaign",
                CampaignObjective.ENGAGEMENT,
                BillingModel.CPC,
                new BigDecimal("2000.00"),
                new BigDecimal("150.00"),
                newStartAt,
                newEndAt,
                createValidTargeting(),
                createValidCreative()
        );

        assertEquals("Updated Campaign", campaign.getName());
        assertEquals(CampaignObjective.ENGAGEMENT, campaign.getObjective());
        assertEquals(BillingModel.CPC, campaign.getBillingModel());
    }

    @Test
    void update_shouldThrowException_whenCampaignIsActive() {
        Campaign campaign = createActiveCampaign();
        Instant newStartAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant newEndAt = newStartAt.plus(10, ChronoUnit.DAYS);

        assertThrows(IllegalStateException.class, () ->
                campaign.update(
                        "Updated Campaign",
                        CampaignObjective.ENGAGEMENT,
                        BillingModel.CPC,
                        new BigDecimal("2000.00"),
                        new BigDecimal("150.00"),
                        newStartAt,
                        newEndAt,
                        createValidTargeting(),
                        createValidCreative()
                )
        );
    }

    private Campaign createCampaign() {
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(7, ChronoUnit.DAYS);
        
        return Campaign.create(
                "partner-123",
                "Test Campaign",
                CampaignObjective.AWARENESS,
                PromotedEntityType.EVENT,
                "event-456",
                BillingModel.CPM,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "XAF",
                startAt,
                endAt,
                createValidTargeting(),
                createValidCreative(),
                "user-789"
        );
    }

    private Campaign createActiveCampaign() {
        Campaign campaign = createCampaign();
        campaign.submit();
        campaign.approve("admin-123");
        campaign.activate();
        return campaign;
    }

    private TargetConfiguration createValidTargeting() {
        TargetConfiguration targeting = new TargetConfiguration();
        targeting.setCountryCodes(List.of("CM"));
        targeting.setMinimumAge(18);
        targeting.setMaximumAge(65);
        return targeting;
    }

    private CreativeConfiguration createValidCreative() {
        CreativeConfiguration creative = new CreativeConfiguration();
        creative.setTitle("Test Ad");
        creative.setDescription("Test Description");
        creative.setCallToAction("LEARN_MORE");
        creative.setImageUrl("https://example.com/image.jpg");
        return creative;
    }
}
