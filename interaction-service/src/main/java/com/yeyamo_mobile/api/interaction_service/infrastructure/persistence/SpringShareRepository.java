package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringShareRepository extends JpaRepository<ShareEntity,UUID>{long countByPostId(UUID postId);}
