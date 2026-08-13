package com.yeyamo_mobile.api.admin_service.models;
import java.time.LocalDateTime;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="admin_scopes") public class AdminScope { public enum Type { GLOBAL, COUNTRY, ADMIN_AREA, CITY }
 @Id public UUID id=UUID.randomUUID();@Column(name="admin_user_id",nullable=false) public UUID adminUserId;@Enumerated(EnumType.STRING)@Column(name="scope_type",nullable=false) public Type scopeType;@Column(name="country_code",length=2) public String countryCode;@Column(name="administrative_area_id") public UUID administrativeAreaId;@Column(name="city_id") public UUID cityId;@Column(name="created_at",nullable=false) public LocalDateTime createdAt=LocalDateTime.now();}
