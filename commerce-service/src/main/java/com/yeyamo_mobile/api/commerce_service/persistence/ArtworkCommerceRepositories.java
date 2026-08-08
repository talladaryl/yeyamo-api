package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public final class ArtworkCommerceRepositories{private ArtworkCommerceRepositories(){}
 public interface Offers extends JpaRepository<ArtworkOffer,UUID>{@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)@Query("select o from ArtworkOffer o where o.id=:id")Optional<ArtworkOffer>locked(@Param("id")UUID id);Optional<ArtworkOffer>findByArtworkId(UUID artworkId);}
 public interface Orders extends JpaRepository<ArtworkOrder,UUID>{Optional<ArtworkOrder>findByIdempotencyKey(String key);Optional<ArtworkOrder>findByCommerceOrderId(UUID id);List<ArtworkOrder>findByBuyerUserIdOrderByCreatedAtDesc(String user);List<ArtworkOrder>findByArtisanPartnerIdOrderByCreatedAtDesc(String partner);@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)@Query("select o from ArtworkOrder o where o.id=:id")Optional<ArtworkOrder>locked(@Param("id")UUID id);}
 public interface History extends JpaRepository<ArtworkOrderHistory,UUID>{List<ArtworkOrderHistory>findByOrderIdOrderByCreatedAtAsc(UUID id);}
}
