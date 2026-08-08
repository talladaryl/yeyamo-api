package com.yeyamo_mobile.api.commerce_service.persistence;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="artwork_order_status_history")public class ArtworkOrderHistory{@Id public UUID id;@Column(name="order_id")public UUID orderId;@Column(name="previous_status")public String previousStatus;@Column(name="new_status")public String newStatus;public String reason;@Column(name="actor_id")public String actorId;@Column(name="created_at")public Instant createdAt;}
