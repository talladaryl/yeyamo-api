package com.yeyamo_mobile.api.auth_service.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(
        name = "oauth_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
)
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OAuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @JoinColumn(name="user_id")
    @ManyToOne
    private User user;

    private String provider;

    @jakarta.persistence.Column(name = "provider_user_id")
    private String providerUserId;
}
