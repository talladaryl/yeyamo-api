package com.yeyamo_mobile.api.event_service.config;

import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI eventOpenApi(){
        return new OpenAPI().info(new Info().title("YeYamo Event API").version("v1"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
    }
}
