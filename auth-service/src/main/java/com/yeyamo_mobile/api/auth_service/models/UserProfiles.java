package com.yeyamo_mobile.api.auth_service.models;

import com.yeyamo_mobile.api.auth_service.enums.Language;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name="user_profiles")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfiles {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String displayName;
    private String avatarUrl;
    private String bio;

    @Enumerated(EnumType.STRING)
    private Language language; 

    @JoinColumn(name="user_id")
    @ManyToOne
    private User user;
}
