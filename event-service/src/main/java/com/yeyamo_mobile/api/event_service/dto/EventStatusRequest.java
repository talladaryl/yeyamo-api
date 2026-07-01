package com.yeyamo_mobile.api.event_service.dto;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;

import jakarta.validation.constraints.NotNull;

public class EventStatusRequest {

    @NotNull
    private EventStatus status;

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}
