package com.yeyamo_mobile.api.gamification_service.infrastructure.persistence;import java.time.Instant;import jakarta.persistence.*;
@Entity@Table(name="gamification_counters")public class CounterEntity{@EmbeddedId CounterId id;@Column(nullable=false)long value;@Column(name="updated_at",nullable=false)Instant updatedAt;@Version long version;}
