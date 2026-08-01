package com.yeyamo_mobile.api.auth_service.config;

import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.models.Role;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;

@Component
@Order(10)
@ConditionalOnProperty(name = "yeyamo.bootstrap-admin.enabled", havingValue = "true")
public class LocalAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalAdminBootstrap.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public LocalAdminBootstrap(
            UserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            @Value("${yeyamo.bootstrap-admin.email:}") String email,
            @Value("${yeyamo.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Local admin bootstrap requires an email and a password of at least 12 characters");
        }

        Role userRole = requiredRole(Roles.USER);
        Role superAdminRole = requiredRole(Roles.SUPER_ADMIN);
        User admin = users.findByEmail(normalizedEmail).orElseGet(User::new);

        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerifiedAt(Instant.now());
        admin.getRoles().add(userRole);
        admin.getRoles().add(superAdminRole);
        users.save(admin);

        log.warn("Local SUPER_ADMIN bootstrap applied for {}. Disable it after provisioning.", normalizedEmail);
    }

    private Role requiredRole(Roles code) {
        return roles.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + code));
    }
}
