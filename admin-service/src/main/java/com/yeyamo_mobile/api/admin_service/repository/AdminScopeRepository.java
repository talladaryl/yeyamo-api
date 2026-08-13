package com.yeyamo_mobile.api.admin_service.repository;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;import com.yeyamo_mobile.api.admin_service.models.AdminScope;
public interface AdminScopeRepository extends JpaRepository<AdminScope,UUID>{List<AdminScope> findByAdminUserId(UUID adminUserId);}
