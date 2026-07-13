package com.yeyamo_mobile.api.media_service.infrastructure.persistence;
import java.util.*;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.media_service.domain.model.MediaAsset;import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;
@Component
public class JpaMediaRepositoryAdapter implements MediaRepository{
 private final SpringMediaRepository repo;public JpaMediaRepositoryAdapter(SpringMediaRepository r){repo=r;}
 public MediaAsset save(MediaAsset m){return domain(repo.save(entity(m)));}public Optional<MediaAsset> findById(UUID id){return repo.findById(id).map(this::domain);}
 public boolean existsByChecksumAndOwnerId(String c,String o){return repo.existsByChecksumAndOwnerId(c,o);}
 private MediaEntity entity(MediaAsset m){MediaEntity e=new MediaEntity();e.setId(m.getId());e.setOwnerId(m.getOwnerId());e.setType(m.getType());e.setStatus(m.getStatus());e.setThumbnailStatus(m.getThumbnailStatus());
  e.setOriginalFilename(m.getOriginalFilename());e.setContentType(m.getContentType());e.setSizeBytes(m.getSizeBytes());e.setChecksum(m.getChecksum());e.setStorageKey(m.getStorageKey());e.setThumbnailKey(m.getThumbnailKey());
  e.setWidth(m.getWidth());e.setHeight(m.getHeight());e.setDurationMs(m.getDurationMs());e.setAltText(m.getAltText());e.setAggregateType(m.getAggregateType());e.setAggregateId(m.getAggregateId());e.setFailureReason(m.getFailureReason());
  e.setCreatedAt(m.getCreatedAt());e.setUpdatedAt(m.getUpdatedAt());e.setDeletedAt(m.getDeletedAt());e.setVersion(m.getVersion());return e;}
 private MediaAsset domain(MediaEntity e){MediaAsset m=new MediaAsset();m.setId(e.getId());m.setOwnerId(e.getOwnerId());m.setType(e.getType());m.setStatus(e.getStatus());m.setThumbnailStatus(e.getThumbnailStatus());
  m.setOriginalFilename(e.getOriginalFilename());m.setContentType(e.getContentType());m.setSizeBytes(e.getSizeBytes());m.setChecksum(e.getChecksum());m.setStorageKey(e.getStorageKey());m.setThumbnailKey(e.getThumbnailKey());
  m.setWidth(e.getWidth());m.setHeight(e.getHeight());m.setDurationMs(e.getDurationMs());m.setAltText(e.getAltText());m.setAggregateType(e.getAggregateType());m.setAggregateId(e.getAggregateId());m.setFailureReason(e.getFailureReason());
  m.setCreatedAt(e.getCreatedAt());m.setUpdatedAt(e.getUpdatedAt());m.setDeletedAt(e.getDeletedAt());m.setVersion(e.getVersion());return m;}
}
