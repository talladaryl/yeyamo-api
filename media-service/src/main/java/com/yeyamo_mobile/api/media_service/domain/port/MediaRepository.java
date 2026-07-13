package com.yeyamo_mobile.api.media_service.domain.port;
import java.util.*;import com.yeyamo_mobile.api.media_service.domain.model.MediaAsset;
public interface MediaRepository{MediaAsset save(MediaAsset media);Optional<MediaAsset> findById(UUID id);boolean existsByChecksumAndOwnerId(String checksum,String ownerId);}
