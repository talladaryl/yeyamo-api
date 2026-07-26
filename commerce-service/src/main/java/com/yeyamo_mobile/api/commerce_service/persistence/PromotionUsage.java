package com.yeyamo_mobile.api.commerce_service.persistence;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="commerce_promotion_usages")public class PromotionUsage{@Id public UUID id;@Column(name="promotion_id")public UUID promotionId;@Column(name="order_id")public UUID orderId;@Column(name="user_id")public String userId;@Column(name="used_at")public Instant usedAt;}
