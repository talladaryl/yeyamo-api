package com.yeyamo_mobile.api.event_service.models;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.shared.geography.GeographicFields;

import jakarta.persistence.Column;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;
    @Column(name="organizer_id") private UUID organizerId;
    @Column(name="partner_id") private UUID partnerId;
    @Column(name="region_id") private Long regionId;
    @Column(name="city_id") private Long cityId;
    @Column(name="category_id") private Long categoryId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventStatus status = EventStatus.PENDING;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount = 0;

    @Column(name = "is_virtual", nullable = false) private boolean virtual;

    @ElementCollection
    @CollectionTable(name = "event_accessible_countries", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "country_code", length = 2)
    private java.util.Set<String> accessibleCountries = new java.util.HashSet<>();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "countryCode", column = @Column(name = "country_code", length = 2)),
        @AttributeOverride(name = "adminLevel1Id", column = @Column(name = "admin_level_1_id")),
        @AttributeOverride(name = "adminLevel2Id", column = @Column(name = "admin_level_2_id")),
        @AttributeOverride(name = "cityId", column = @Column(name = "country_city_id")),
        @AttributeOverride(name = "localityId", column = @Column(name = "locality_id")),
        @AttributeOverride(name = "latitude", column = @Column(name = "latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "longitude")),
        @AttributeOverride(name = "languageCode", column = @Column(name = "language_code", length = 10))
    })
    private GeographicFields geography;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
