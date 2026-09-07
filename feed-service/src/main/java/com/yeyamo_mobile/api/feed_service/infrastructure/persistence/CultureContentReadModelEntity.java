package com.yeyamo_mobile.api.feed_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "culture_content_read_model")
public class CultureContentReadModelEntity {
    @Id
    @Column(name = "content_id")
    private UUID contentId;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(length = 255)
    private String title;
    @Column(name = "is_active", nullable = false)
    private boolean active;

    public UUID getContentId() { return contentId; }
    public void setContentId(UUID value) { contentId = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
