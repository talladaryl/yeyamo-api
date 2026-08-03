package com.yeyamo_mobile.api.culture_service.infrastructure.persistence;
import org.springframework.context.annotation.Configuration;import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@Configuration @EnableJpaRepositories(basePackageClasses=CultureRepositories.class,considerNestedRepositories=true) public class JpaConfig {}
