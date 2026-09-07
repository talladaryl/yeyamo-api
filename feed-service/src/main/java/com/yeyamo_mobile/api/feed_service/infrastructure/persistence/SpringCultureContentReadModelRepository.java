package com.yeyamo_mobile.api.feed_service.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringCultureContentReadModelRepository extends JpaRepository<CultureContentReadModelEntity, UUID> { }
