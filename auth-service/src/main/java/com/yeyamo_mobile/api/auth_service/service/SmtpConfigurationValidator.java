package com.yeyamo_mobile.api.auth_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmtpConfigurationValidator {
    public SmtpConfigurationValidator(
            EmailSenderProperties properties,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        if (properties.deliveryEnabled()) {
            require(host, "MAIL_HOST");
            require(username, "MAIL_USERNAME");
            require(password, "MAIL_PASSWORD");
        }
    }

    private void require(String value, String variable) {
        if (value == null || value.isBlank()) throw new IllegalStateException(variable + " is required when email delivery is enabled");
    }
}
