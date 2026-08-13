package com.yeyamo_mobile.api.country_config_service.domain.model;

/**
 * Launch status for a country in the YeYamo platform.
 * 
 * Controls progressive rollout and feature availability.
 */
public enum CountryLaunchStatus {
    /**
     * Country is not yet available. All features disabled.
     */
    DISABLED,
    
    /**
     * Country is planned for future launch. May appear in selection lists
     * but all functional features are disabled.
     */
    COMING_SOON,
    
    /**
     * Country is in beta testing with limited features and user access.
     */
    BETA,
    
    /**
     * Country is fully live and operational.
     * Cameroon is the only LIVE country at initial launch.
     */
    LIVE
}
