package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringCommandReceiptRepository extends JpaRepository<CommandReceiptEntity,UUID>{Optional<CommandReceiptEntity>findByIdempotencyKeyAndActorIdAndOperation(String key,String actor,String operation);}
