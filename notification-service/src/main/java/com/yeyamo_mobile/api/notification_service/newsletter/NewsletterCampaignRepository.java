package com.yeyamo_mobile.api.notification_service.newsletter;
import java.time.Instant;import java.util.*;import org.springframework.data.jpa.repository.*;
public interface NewsletterCampaignRepository extends JpaRepository<NewsletterCampaignEntity,UUID>,JpaSpecificationExecutor<NewsletterCampaignEntity>{@Query("select c from NewsletterCampaignEntity c where c.status='SCHEDULED' and c.scheduledAt<=:now")List<NewsletterCampaignEntity> findDue(Instant now);}
