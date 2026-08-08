package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

/**
 * Reasons for reporting content
 */
public enum ReportReason {
    // General reasons
    SPAM,
    HARASSMENT,
    HATE_SPEECH,
    VIOLENCE,
    NUDITY,
    FRAUD,
    MISINFORMATION,
    
    // Legal and IP reasons
    COPYRIGHT,
    COPYRIGHT_INFRINGEMENT,
    PLAGIARISM,
    
    // Cultural content specific
    CULTURAL_MISREPRESENTATION,
    FALSE_AUTHENTICITY,
    MISLEADING_HISTORY,
    SACRED_CONTENT,
    COMMUNITY_RESTRICTED_CONTENT,
    
    // Privacy
    PERSONAL_DATA,
    NO_CONSENT,
    
    // Artisan/Artwork specific
    COUNTERFEIT_ARTWORK,
    
    OTHER
}
