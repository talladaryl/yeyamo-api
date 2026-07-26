package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketQrCredentialRepository extends JpaRepository<TicketQrCredentialEntity, UUID> {
    
    Optional<TicketQrCredentialEntity> findByTicketId(UUID ticketId);
    
    Optional<TicketQrCredentialEntity> findByTokenId(String tokenId);
    
    @Query("SELECT c FROM TicketQrCredentialEntity c WHERE c.tokenId = :tokenId AND c.revokedAt IS NULL")
    Optional<TicketQrCredentialEntity> findActiveByTokenId(@Param("tokenId") String tokenId);
}
