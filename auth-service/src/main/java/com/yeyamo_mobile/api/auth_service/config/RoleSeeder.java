package com.yeyamo_mobile.api.auth_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.auth_service.enums.LabelRole;
import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.models.Role;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;

@Component
@Order(0)
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        seed(Roles.USER, LabelRole.UTILISATEUR);
        seed(Roles.PARTNER, LabelRole.PARTENAIRE);
        seed(Roles.ADMIN, LabelRole.COMMERCIAL);
        seed(Roles.SUPER_ADMIN, LabelRole.SUPER_ADMINISTRATEUR);
        seed(Roles.MODERATOR, LabelRole.MODERATEUR);
        seed(Roles.EDITOR, LabelRole.MODERATEUR);
        seed(Roles.SUPPORT, LabelRole.UTILISATEUR);
        seed(Roles.COMMERCIAL, LabelRole.COMMERCIAL);
    }

    private void seed(Roles code, LabelRole label) {
        roleRepository.findByCode(code)
                .orElseGet(() -> roleRepository.save(new Role(null, code, label)));
    }
}
