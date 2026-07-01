package com.yeyamo_mobile.api.place_service.dto;

import java.time.LocalTime;

import com.yeyamo_mobile.api.place_service.models.PlaceSchedule;

public record PlaceScheduleResponse(
        Long id,
        Integer dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
) {
    public static PlaceScheduleResponse from(PlaceSchedule schedule) {
        return new PlaceScheduleResponse(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getOpenTime(),
                schedule.getCloseTime()
        );
    }
}
