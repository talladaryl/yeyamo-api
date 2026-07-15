package com.yeyamo.security.hardening;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import jakarta.servlet.Filter;

@AutoConfiguration
@ConditionalOnClass(Filter.class)
@EnableConfigurationProperties(HardeningProperties.class)
@ConditionalOnProperty(prefix = "yeyamo.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HardeningAutoConfiguration {

    @Bean
    static BeanPostProcessor strictJwtDecoderPostProcessor(Environment environment) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof JwtDecoder)
                        || !environment.getProperty("yeyamo.security.jwt-validation.enabled", Boolean.class, true)) {
                    return bean;
                }
                return StrictJwtDecoders.create(
                        environment.getProperty("jwt.secret"),
                        environment.getProperty("jwt.previous-secrets", ""),
                        environment.getProperty("jwt.jwk-set-uri", ""),
                        environment.getProperty("jwt.issuer", "https://auth.yeyamo.internal"),
                        environment.getProperty("jwt.audience", "yeyamo-api"));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(SecurityHardeningFilter.class)
    SecurityHardeningFilter securityHardeningFilter(HardeningProperties properties) {
        return new SecurityHardeningFilter(properties);
    }

    @Bean
    FilterRegistrationBean<SecurityHardeningFilter> securityHardeningFilterRegistration(
            SecurityHardeningFilter filter) {
        FilterRegistrationBean<SecurityHardeningFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("yeyamoSecurityHardeningFilter");
        return registration;
    }
}
