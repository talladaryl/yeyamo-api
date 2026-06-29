package com.yeyamo_mobile.api.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.models.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(Roles code);
}
