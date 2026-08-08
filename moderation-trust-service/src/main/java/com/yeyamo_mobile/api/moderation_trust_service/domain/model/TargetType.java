package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

/**
 * Types of content that can be reported or moderated
 */
public enum TargetType {
    // Social content
    POST, 
    COMMENT, 
    MEDIA, 
    MESSAGE, 
    
    // Actors
    USER, 
    PARTNER, 
    
    // Commerce
    CATALOG_ASSET,
    
    // Culture & Artisan content
    ARTWORK,
    ARTISAN,
    CULTURE_CONTENT,
    CULTURE_TRANSLATION,
    LANGUAGE_AUDIO,
    CULTURE_CHALLENGE_SUBMISSION,
    
    // Legal claims
    AUTHENTICITY_CLAIM,
    COPYRIGHT_CLAIM
}
