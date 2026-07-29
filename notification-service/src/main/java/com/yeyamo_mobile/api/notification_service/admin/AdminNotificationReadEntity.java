package com.yeyamo_mobile.api.notification_service.admin;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="admin_notification_reads")public class AdminNotificationReadEntity{@Id public UUID id;@Column(name="notification_id")public UUID notificationId;@Column(name="admin_id")public String adminId;@Column(name="read_at")public Instant readAt;}
