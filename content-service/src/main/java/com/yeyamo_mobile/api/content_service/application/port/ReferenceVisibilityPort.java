package com.yeyamo_mobile.api.content_service.application.port;
import com.yeyamo_mobile.api.content_service.domain.model.PostReferenceType;
import com.yeyamo_mobile.api.content_service.domain.model.CulturalTargetType;
import java.util.UUID;
public interface ReferenceVisibilityPort { void requirePublic(PostReferenceType type,String id); default void requireCultureTarget(CulturalTargetType type,UUID id){requirePublic(PostReferenceType.CULTURE_CONTENT,id.toString());} }
