package com.yeyamo.security.hardening;

import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Supplies a consistent bearer-auth scheme to every service that enables Springdoc. */
@AutoConfiguration(after = SpringDocConfiguration.class)
@ConditionalOnClass(OpenAPI.class)
public class OpenApiDocumentationConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    OpenAPI yeyamoOpenApi(@Value("${spring.application.name:YeYamo API}") String applicationName) {
        return new OpenAPI()
                .info(new Info().title("YeYamo – " + applicationName).version("v1"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
