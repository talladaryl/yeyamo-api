package com.yeyamo_mobile.api.notification_service.newsletter;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;
public interface NewsletterCampaignStatsRepository extends JpaRepository<NewsletterCampaignStatsEntity,UUID>{}
