package com.yeyamo_mobile.api.commerce_service.interfaces;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(info = @Info(
        title = "YeYamo Artwork Commerce API",
        version = "v1",
        description = "Artwork offers, orders, payments and fulfilment workflow."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class CommerceOpenApiConfig {
}
