package com.yeyamo_mobile.api.analytics_service.business;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "analytics_reach_fingerprints")
public class ReachFingerprint {
    @Id public String fingerprint;
    public LocalDate statDate;
    public String campaignId;
}
