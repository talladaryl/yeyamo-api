package com.yeyamo_mobile.api.auth_service.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.auth_service.dto.AdminPlatformUserDetail;
import com.yeyamo_mobile.api.auth_service.dto.AdminPlatformUserSummary;
import com.yeyamo_mobile.api.auth_service.dto.AdminRevokeSessionsRequest;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserRolesRequest;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserSessionResponse;
import com.yeyamo_mobile.api.auth_service.dto.AdminUserStatusRequest;
import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.event.AuthEventOutbox;
import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.AdminUserProfileProjection;
import com.yeyamo_mobile.api.auth_service.models.RefreshToken;
import com.yeyamo_mobile.api.auth_service.models.Role;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.AdminUserProfileProjectionRepository;
import com.yeyamo_mobile.api.auth_service.repository.RefreshTokenRepository;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
public class AdminPlatformUserService {
    private static final Set<Roles> PLATFORM_ROLES = EnumSet.of(Roles.USER, Roles.PARTNER);
    private static final Set<UserStatus> ADMIN_STATUSES = EnumSet.of(UserStatus.ACTIVE, UserStatus.SUSPENDED,
            UserStatus.BLOCKED, UserStatus.DEACTIVATED);
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository sessions;
    private final AdminUserProfileProjectionRepository profiles;
    private final AuthEventOutbox outbox;

    public AdminPlatformUserService(UserRepository users, RoleRepository roles, RefreshTokenRepository sessions,
            AdminUserProfileProjectionRepository profiles, AuthEventOutbox outbox) {
        this.users = users;
        this.roles = roles;
        this.sessions = sessions;
        this.profiles = profiles;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public Page<AdminPlatformUserSummary> list(String search, String email, String phone, UserStatus status,
            Roles role, Long regionId, LocalDateTime createdFrom, LocalDateTime createdTo,
            Instant lastLoginFrom, Instant lastLoginTo, Pageable pageable) {
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 200),
                pageable.getSort());
        Page<User> page = users.findAll(specification(search, email, phone, status, role, regionId, createdFrom,
                createdTo, lastLoginFrom, lastLoginTo), safePageable);
        Map<Long, AdminUserProfileProjection> projectionByUser = profileMap(page.getContent());
        return page.map(user -> summary(user, projectionByUser.get(user.getId())));
    }

    @Transactional(readOnly = true)
    public AdminPlatformUserDetail detail(Long id) {
        User user = get(id);
        AdminUserProfileProjection profile = profiles.findById(id).orElse(null);
        return new AdminPlatformUserDetail(summary(user, profile), sessions.countByUserAndRevokedAtIsNull(user),
                profile != null, null, null, null, null);
    }

    @Transactional
    public AdminPlatformUserDetail changeStatus(Long id, AdminUserStatusRequest request, Authentication actor,
            String correlationId) {
        if (!ADMIN_STATUSES.contains(request.status())) {
            throw invalid("USER_STATUS_NOT_ADMINISTRABLE", "Statut non autorise par cette API");
        }
        User user = get(id);
        UserStatus previous = user.getStatus();
        user.setStatus(request.status());
        users.save(user);
        if (request.status() != UserStatus.ACTIVE) sessions.deleteByUser(user);
        outbox.adminChanged(user, "admin.user_status_changed", actor.getName(), request.reason(), correlationId,
                Map.of("previousStatus", previous.name(), "status", request.status().name()));
        return detail(id);
    }

    @Transactional
    public AdminPlatformUserDetail changeRoles(Long id, AdminUserRolesRequest request, Authentication actor,
            String correlationId) {
        if (!PLATFORM_ROLES.containsAll(request.roles())) {
            throw invalid("INVALID_PLATFORM_ROLE", "Seuls USER et PARTNER sont geres par cette API");
        }
        User user = get(id);
        Set<Role> assigned = request.roles().stream().map(code -> roles.findByCode(code)
                .orElseThrow(() -> invalid("ROLE_NOT_CONFIGURED", "Role non configure: " + code))).collect(Collectors.toSet());
        Set<String> previous = user.getRoles().stream().map(role -> role.getCode().name()).collect(Collectors.toSet());
        user.setRoles(assigned);
        users.save(user);
        outbox.adminChanged(user, "admin.user_roles_changed", actor.getName(), request.reason(), correlationId,
                Map.of("previousRoles", previous, "roles", request.roles()));
        return detail(id);
    }

    @Transactional(readOnly = true)
    public List<AdminUserSessionResponse> sessions(Long id) {
        User user = get(id);
        LocalDateTime now = LocalDateTime.now();
        return sessions.findByUserOrderByIdDesc(user).stream().map(token -> session(token, now)).toList();
    }

    @Transactional
    public void revokeSessions(Long id, AdminRevokeSessionsRequest request, Authentication actor, String correlationId) {
        User user = get(id);
        if (request.sessionId() == null) {
            sessions.deleteByUser(user);
        } else {
            RefreshToken token = sessions.findByIdAndUser(request.sessionId(), user)
                    .orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "Session introuvable", HttpStatus.NOT_FOUND));
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                sessions.save(token);
            }
        }
        outbox.adminChanged(user, "admin.user_sessions_revoked", actor.getName(), request.reason(), correlationId,
                request.sessionId() == null ? Map.of("scope", "ALL") : Map.of("sessionId", request.sessionId()));
    }

    @Transactional(readOnly = true)
    public void export(OutputStream output, String search, UserStatus status, Roles role, Long regionId,
            String actor, String correlationId) throws IOException {
        output.write("id,displayName,email,phone,status,roles,regionId,createdAt,lastLoginAt\n".getBytes(StandardCharsets.UTF_8));
        int pageNumber = 0;
        Page<User> page;
        do {
            page = users.findAll(specification(search, null, null, status, role, regionId, null, null, null, null),
                    PageRequest.of(pageNumber++, 500));
            Map<Long, AdminUserProfileProjection> projections = profileMap(page.getContent());
            for (User user : page.getContent()) {
                AdminPlatformUserSummary row = summary(user, projections.get(user.getId()));
                String csv = String.join(",", csv(row.id()), csv(row.displayName()), csv(row.email()), csv(row.phone()),
                        csv(row.status()), csv(String.join("|", row.roles())), csv(row.regionId()), csv(row.createdAt()),
                        csv(row.lastLoginAt())) + "\n";
                output.write(csv.getBytes(StandardCharsets.UTF_8));
            }
        } while (page.hasNext());
        Map<String, Object> filters = new LinkedHashMap<>();
        if (hasText(search)) filters.put("search", search);
        if (status != null) filters.put("status", status.name());
        if (role != null) filters.put("role", role.name());
        if (regionId != null) filters.put("regionId", regionId);
        outbox.adminExport(actor, correlationId, filters);
    }

    private Specification<User> specification(String search, String email, String phone, UserStatus status, Roles role,
            Long regionId, LocalDateTime createdFrom, LocalDateTime createdTo, Instant lastLoginFrom, Instant lastLoginTo) {
        return (root, query, builder) -> {
            List<Predicate> filters = new java.util.ArrayList<>();
            if (hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                var profileSubquery = query.subquery(Long.class);
                var profile = profileSubquery.from(AdminUserProfileProjection.class);
                profileSubquery.select(profile.get("authUserId")).where(builder.like(builder.lower(profile.get("displayName")), pattern));
                filters.add(builder.or(builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("phone")), pattern), root.get("id").in(profileSubquery)));
            }
            if (hasText(email)) filters.add(builder.equal(builder.lower(root.get("email")), email.trim().toLowerCase()));
            if (hasText(phone)) filters.add(builder.equal(root.get("phone"), phone.trim()));
            if (status != null) filters.add(builder.equal(root.get("status"), status));
            if (role != null) filters.add(builder.equal(root.join("roles", JoinType.INNER).get("code"), role));
            if (regionId != null) {
                var profileSubquery = query.subquery(Long.class);
                var profile = profileSubquery.from(AdminUserProfileProjection.class);
                profileSubquery.select(profile.get("authUserId")).where(builder.equal(profile.get("regionId"), regionId));
                filters.add(root.get("id").in(profileSubquery));
            }
            if (createdFrom != null) filters.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            if (createdTo != null) filters.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            if (lastLoginFrom != null) filters.add(builder.greaterThanOrEqualTo(root.get("lastLoginAt"), lastLoginFrom));
            if (lastLoginTo != null) filters.add(builder.lessThanOrEqualTo(root.get("lastLoginAt"), lastLoginTo));
            query.distinct(true);
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }

    private Map<Long, AdminUserProfileProjection> profileMap(Collection<User> page) {
        return profiles.findByAuthUserIdIn(page.stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(AdminUserProfileProjection::getAuthUserId, Function.identity()));
    }

    private AdminPlatformUserSummary summary(User user, AdminUserProfileProjection profile) {
        String displayName = profile == null ? null : profile.getDisplayName();
        String username = displayName == null ? user.getEmail() : displayName;
        return new AdminPlatformUserSummary(user.getId(), displayName, username, user.getEmail(), user.getPhone(),
                profile == null ? null : profile.getAvatarUrl(), user.getStatus(),
                user.getRoles().stream().map(role -> role.getCode().name()).collect(Collectors.toSet()),
                profile == null ? null : profile.getRegionId(), user.getCreatedAt(), user.getLastLoginAt());
    }

    private AdminUserSessionResponse session(RefreshToken token, LocalDateTime now) {
        return new AdminUserSessionResponse(token.getId(), null, null, null, null, token.getExpiresAt(),
                token.getRevokedAt() == null && token.getExpiresAt().isAfter(now));
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException("PLATFORM_USER_NOT_FOUND",
                "Utilisateur plateforme introuvable", HttpStatus.NOT_FOUND));
    }

    private ApiException invalid(String code, String message) {
        return new ApiException(code, message, HttpStatus.BAD_REQUEST);
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private String csv(Object value) {
        if (value == null) return "";
        return "\"" + value.toString().replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
