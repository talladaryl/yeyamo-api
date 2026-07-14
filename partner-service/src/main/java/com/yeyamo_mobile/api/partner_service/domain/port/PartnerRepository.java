package com.yeyamo_mobile.api.partner_service.domain.port;
import java.util.Optional;import java.util.UUID;import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;import com.yeyamo_mobile.api.partner_service.domain.model.Partner;
public interface PartnerRepository { Partner save(Partner p); Optional<Partner> findById(UUID id); Optional<Partner> findByOwnerUserId(String id); boolean existsByOwnerUserId(String id); Page<Partner> searchApproved(String q,Pageable pageable); }
