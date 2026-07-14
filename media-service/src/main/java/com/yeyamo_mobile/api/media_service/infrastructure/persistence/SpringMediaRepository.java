package com.yeyamo_mobile.api.media_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringMediaRepository extends JpaRepository<MediaEntity,UUID>{boolean existsByChecksumAndOwnerId(String checksum,String ownerId);}
