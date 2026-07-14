package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;import java.time.Instant;import jakarta.persistence.*;
@Entity@Table(name="recommendation_user_signals")public class SignalEntity{@EmbeddedId SignalId id;@Column(nullable=false)double weight;@Column(name="last_interaction_at",nullable=false)Instant lastInteractionAt;@Version long version;}
