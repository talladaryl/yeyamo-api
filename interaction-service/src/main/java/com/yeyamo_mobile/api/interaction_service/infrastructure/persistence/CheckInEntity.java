package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="interaction_checkins")
public class CheckInEntity{@Id private UUID id;@Column(name="catalog_asset_id",nullable=false)private UUID catalogAssetId;@Column(name="user_id",nullable=false,length=100)private String userId;private Double latitude;private Double longitude;@Column(nullable=false)private boolean visible;@Column(name="occurred_at",nullable=false)private Instant occurredAt;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public UUID getCatalogAssetId(){return catalogAssetId;}public void setCatalogAssetId(UUID v){catalogAssetId=v;}public String getUserId(){return userId;}public void setUserId(String v){userId=v;}
 public Double getLatitude(){return latitude;}public void setLatitude(Double v){latitude=v;}public Double getLongitude(){return longitude;}public void setLongitude(Double v){longitude=v;}public boolean isVisible(){return visible;}public void setVisible(boolean v){visible=v;}public Instant getOccurredAt(){return occurredAt;}public void setOccurredAt(Instant v){occurredAt=v;}}
