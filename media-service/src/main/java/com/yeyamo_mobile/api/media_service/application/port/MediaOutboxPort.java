package com.yeyamo_mobile.api.media_service.application.port;
import com.yeyamo_mobile.api.media_service.domain.model.MediaAsset;
public interface MediaOutboxPort{void append(String eventType,MediaAsset media,String correlationId,String actorId);}
