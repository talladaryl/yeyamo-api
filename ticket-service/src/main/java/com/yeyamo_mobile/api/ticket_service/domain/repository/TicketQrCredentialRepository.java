package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketQrCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketQrCredentialRepository extends JpaRepository<TicketQrCredential, String> {
    
    Optional<TicketQrCredential> findByTicketId(String ticketId);
    
    Optional<TicketQrCredential> findByTokenId(String tokenId);
    
    @Query("SELECT c FROM TicketQrCredential c WHERE c.tokenHash = :tokenHash")
    Optional<TicketQrCredential> findByTokenHash(String tokenHash);
    
    List<TicketQrCredential> findByKeyId(String keyId);
    
    @Query("SELECT c FROM TicketQrCredential c WHERE c.revokedAt IS NOT NULL")
    List<TicketQrCredential> findRevoked();
    
    @Query("SELECT c FROM TicketQrCredential c WHERE c.expiresAt < :now AND c.revokedAt IS NULL")
    List<TicketQrCredential> findExpiredNotRevoked(Instant now);
    
    boolean existsByTicketId(String ticketId);
}
