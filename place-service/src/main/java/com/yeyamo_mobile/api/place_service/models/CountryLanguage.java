package com.yeyamo_mobile.api.place_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

@Entity
@Table(name = "country_languages")
public class CountryLanguage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;
    @Column(name = "language_code", nullable = false, length = 35)
    private String languageCode;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Column(name = "is_official", nullable = false)
    private boolean official;
    @Column(name = "is_primary", nullable = false)
    private boolean primaryLanguage;

    public String getLanguageCode() { return languageCode; }
    public String getDisplayName() { return displayName; }
    public boolean isOfficial() { return official; }
    public boolean isPrimaryLanguage() { return primaryLanguage; }
}
