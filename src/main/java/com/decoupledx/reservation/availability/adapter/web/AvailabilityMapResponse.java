package com.decoupledx.reservation.availability.adapter.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

record AvailabilityMapResponse(
        LocalDate date,
        LocalTime startTime,
        int durationMinutes,
        List<ResourceStatusResponse> resources) {
}
