package com.yeyamo_mobile.api.content_service.application.port;
import com.yeyamo_mobile.api.content_service.domain.model.Post;
public interface ContentOutboxPort{void append(String eventType,Post post,String correlationId,String actorId);}
