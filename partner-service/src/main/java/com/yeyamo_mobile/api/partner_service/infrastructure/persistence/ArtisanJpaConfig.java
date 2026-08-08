package com.yeyamo_mobile.api.partner_service.infrastructure.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.yeyamo_mobile.api.partner_service.infrastructure",
        considerNestedRepositories = true)
public class ArtisanJpaConfig {}
