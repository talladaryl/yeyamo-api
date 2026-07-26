package com.yeyamo_mobile.api.partner_service.infrastructure.staff;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="partner_staff_audit")public class PartnerStaffAuditEntity{@Id public UUID id;@Column(name="partner_id")public UUID partnerId;@Column(name="actor_user_id")public String actorUserId;@Column(name="target_user_id")public String targetUserId;public String action;public String details;@Column(name="occurred_at")public Instant occurredAt;}
