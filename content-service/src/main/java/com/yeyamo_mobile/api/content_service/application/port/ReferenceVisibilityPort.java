package com.yeyamo_mobile.api.content_service.application.port;
import com.yeyamo_mobile.api.content_service.domain.model.PostReferenceType;
public interface ReferenceVisibilityPort { void requirePublic(PostReferenceType type,String id); }
