package com.yeyamo_mobile.api.auth_service.event;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;public interface AuthProcessedEventRepository extends JpaRepository<AuthProcessedEvent,UUID>{}
