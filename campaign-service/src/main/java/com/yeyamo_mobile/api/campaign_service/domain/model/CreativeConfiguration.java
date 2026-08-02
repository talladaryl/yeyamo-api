package com.yeyamo_mobile.api.campaign_service.domain.model;

import java.util.Objects;

public class CreativeConfiguration {
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String callToAction;
    private String destinationUrl;

    public CreativeConfiguration() {
    }

    public void validate() {
        if (isBlank(title) && isBlank(description) && isBlank(imageUrl) && isBlank(videoUrl)) {
            throw new IllegalArgumentException("Creative must have at least title, description, image, or video");
        }
        if (!isBlank(imageUrl) && !isBlank(videoUrl)) {
            throw new IllegalArgumentException("Creative cannot have both image and video");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getCallToAction() { return callToAction; }
    public void setCallToAction(String callToAction) { this.callToAction = callToAction; }
    public String getDestinationUrl() { return destinationUrl; }
    public void setDestinationUrl(String destinationUrl) { this.destinationUrl = destinationUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreativeConfiguration that = (CreativeConfiguration) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(imageUrl, that.imageUrl) &&
                Objects.equals(videoUrl, that.videoUrl) &&
                Objects.equals(callToAction, that.callToAction) &&
                Objects.equals(destinationUrl, that.destinationUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, imageUrl, videoUrl, callToAction, destinationUrl);
    }
}
