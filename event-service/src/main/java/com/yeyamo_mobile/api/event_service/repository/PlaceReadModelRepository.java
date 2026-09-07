package com.yeyamo_mobile.api.event_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.event_service.models.PlaceReadModel;

public interface PlaceReadModelRepository extends JpaRepository<PlaceReadModel, UUID> {}
