package com.yeyamo_mobile.api.content_service.infrastructure.config;
import org.springframework.context.annotation.Configuration;import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.info.Info;import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
@Configuration @OpenAPIDefinition(info=@Info(title="YeYamo Content API",version="v1",description="Draft and social publication lifecycle"))
@SecurityScheme(name="bearerAuth",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
public class OpenApiConfig{}
